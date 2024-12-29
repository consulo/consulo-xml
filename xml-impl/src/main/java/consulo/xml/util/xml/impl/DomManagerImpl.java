/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.util.xml.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.ServiceManager;
import consulo.language.pom.PomManager;
import consulo.language.pom.PomModel;
import consulo.language.pom.PomModelAspect;
import consulo.language.pom.TreeAspect;
import consulo.language.pom.event.PomModelEvent;
import consulo.language.pom.event.PomModelListener;
import consulo.language.pom.event.TreeChangeEvent;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiManager;
import consulo.language.sem.SemKey;
import consulo.language.sem.SemService;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.proxy.advanced.AdvancedProxyBuilder;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Condition;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFileMoveEvent;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import consulo.xml.dom.util.proxy.InvocationHandlerOwner;
import consulo.xml.ide.highlighter.DomSupportEnabled;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.events.DomEvent;
import consulo.xml.util.xml.reflect.AbstractDomChildrenDescription;
import consulo.xml.util.xml.reflect.DomGenericInfo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public final class DomManagerImpl extends DomManager
{
	private static final Key<Object> MOCK = Key.create("MockElement");

	static final Key<WeakReference<DomFileElementImpl>> CACHED_FILE_ELEMENT = Key.create("CACHED_FILE_ELEMENT");
	static final Key<DomFileDescription> MOCK_DESCRIPTION = Key.create("MockDescription");
	static final SemKey<FileDescriptionCachedValueProvider> FILE_DESCRIPTION_KEY = SemKey.createKey("FILE_DESCRIPTION_KEY");
	public static final SemKey<DomInvocationHandler> DOM_HANDLER_KEY = SemKey.createKey("DOM_HANDLER_KEY");
	static final SemKey<IndexedElementInvocationHandler> DOM_INDEXED_HANDLER_KEY = DOM_HANDLER_KEY.subKey("DOM_INDEXED_HANDLER_KEY");
	static final SemKey<CollectionElementInvocationHandler> DOM_COLLECTION_HANDLER_KEY = DOM_HANDLER_KEY.subKey("DOM_COLLECTION_HANDLER_KEY");
	static final SemKey<CollectionElementInvocationHandler> DOM_CUSTOM_HANDLER_KEY = DOM_HANDLER_KEY.subKey("DOM_CUSTOM_HANDLER_KEY");
	static final SemKey<AttributeChildInvocationHandler> DOM_ATTRIBUTE_HANDLER_KEY = DOM_HANDLER_KEY.subKey("DOM_ATTRIBUTE_HANDLER_KEY");

	private final EventDispatcher<DomEventListener> myListeners = EventDispatcher.create(DomEventListener.class);

	private final Project myProject;
	private final SemService mySemService;
	private final DomApplicationComponent myApplicationComponent;

	private boolean myChanging;

	@Inject
	public DomManagerImpl(Project project, VirtualFileManager virtualFileManager)
	{
		super(project);
		myProject = project;
		mySemService = SemService.getSemService(project);
		myApplicationComponent = DomApplicationComponent.getInstance();

		final PomModel pomModel = PomManager.getModel(project);
		pomModel.addModelListener(new PomModelListener()
		{
			@Override
			public void modelChanged(PomModelEvent event)
			{
				if(myChanging)
				{
					return;
				}

				TreeChangeEvent changeSet = (TreeChangeEvent) event.getChangeSet(pomModel.getModelAspect(TreeAspect.class));
				if(changeSet != null)
				{
					PsiFile file = changeSet.getRootElement().getPsi().getContainingFile();
					if(file instanceof XmlFile)
					{
						DomFileElementImpl<DomElement> element = getCachedFileElement((XmlFile) file);
						if(element != null)
						{
							fireEvent(new DomEvent(element, false));
						}
					}
				}
			}

			@Override
			public boolean isAspectChangeInteresting(PomModelAspect aspect)
			{
				return aspect instanceof TreeAspect;
			}
		}, project);

		virtualFileManager.addVirtualFileListener(new VirtualFileListener()
		{
			private final List<DomEvent> myDeletionEvents = new SmartList<>();

			@Override
			public void contentsChanged(@Nonnull VirtualFileEvent event)
			{
				if(!event.isFromSave())
				{
					fireEvents(calcDomChangeEvents(event.getFile()));
				}
			}

			@Override
			public void fileMoved(@Nonnull VirtualFileMoveEvent event)
			{
				fireEvents(calcDomChangeEvents(event.getFile()));
			}

			@Override
			public void beforeFileDeletion(@Nonnull final VirtualFileEvent event)
			{
				myDeletionEvents.addAll(calcDomChangeEvents(event.getFile()));
			}

			@Override
			public void fileDeleted(@Nonnull VirtualFileEvent event)
			{
				if(!myDeletionEvents.isEmpty())
				{
					fireEvents(myDeletionEvents);
					myDeletionEvents.clear();
				}
			}

			@Override
			public void propertyChanged(@Nonnull VirtualFilePropertyEvent event)
			{
				final VirtualFile file = event.getFile();
				if(!file.isDirectory() && VirtualFile.PROP_NAME.equals(event.getPropertyName()))
				{
					fireEvents(calcDomChangeEvents(file));
				}
			}
		}, myProject);
	}

	public long getPsiModificationCount()
	{
		return PsiManager.getInstance(getProject()).getModificationTracker().getModificationCount();
	}

	public <T extends DomInvocationHandler> void cacheHandler(SemKey<T> key, XmlElement element, T handler)
	{
		mySemService.setCachedSemElement(key, element, handler);
	}

	private PsiFile getCachedPsiFile(VirtualFile file)
	{
		return PsiManager.getInstance(myProject).findCachedFile(file);
	}

	private List<DomEvent> calcDomChangeEvents(final VirtualFile file)
	{
		if(!(file instanceof VirtualFileWithId) || myProject.isDisposed())
		{
			return Collections.emptyList();
		}

		final List<DomEvent> events = new ArrayList<>();
		VirtualFileUtil.visitChildrenRecursively(file, new VirtualFileVisitor()
		{
			@Override
			public boolean visitFile(@Nonnull VirtualFile file)
			{
				if(myProject.isDisposed() || !ProjectFileIndex.getInstance(myProject).isInContent(file))
				{
					return false;
				}

				if(!file.isDirectory() && XmlFileType.INSTANCE == file.getFileType())
				{
					final PsiFile psiFile = getCachedPsiFile(file);
					if(psiFile != null && XmlFileType.INSTANCE.equals(psiFile.getFileType()) && psiFile instanceof XmlFile)
					{
						final DomFileElementImpl domElement = getCachedFileElement((XmlFile) psiFile);
						if(domElement != null)
						{
							events.add(new DomEvent(domElement, false));
						}
					}
				}
				return true;
			}

			@Nullable
			@Override
			public Iterable<VirtualFile> getChildrenIterable(@Nonnull VirtualFile file)
			{
				return ((NewVirtualFile) file).getCachedChildren();
			}
		});
		return events;
	}

	@SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass"})
	public static DomManagerImpl getDomManager(Project project)
	{
		return (DomManagerImpl) DomManager.getDomManager(project);
	}

	@Override
	public void addDomEventListener(DomEventListener listener, Disposable parentDisposable)
	{
		myListeners.addListener(listener, parentDisposable);
	}

	@Override
	public final ConverterManager getConverterManager()
	{
		return ServiceManager.getService(ConverterManager.class);
	}

	@Override
	public final ModelMerger createModelMerger()
	{
		return new ModelMergerImpl();
	}

	final void fireEvent(DomEvent event)
	{
		if(mySemService.isInsideAtomicChange())
		{
			return;
		}
		incModificationCount();
		myListeners.getMulticaster().eventOccured(event);
	}

	private void fireEvents(Collection<DomEvent> events)
	{
		for(DomEvent event : events)
		{
			fireEvent(event);
		}
	}

	@Override
	public final DomGenericInfo getGenericInfo(final Type type)
	{
		return myApplicationComponent.getStaticGenericInfo(type);
	}

	@Nullable
	public static DomInvocationHandler getDomInvocationHandler(DomElement proxy)
	{
		if(proxy instanceof DomFileElement)
		{
			return null;
		}
		if(proxy instanceof DomInvocationHandler)
		{
			return (DomInvocationHandler) proxy;
		}
		final InvocationHandler handler = InvocationHandlerOwner.getHandler(proxy);
		if(handler instanceof StableInvocationHandler)
		{
			//noinspection unchecked
			final DomElement element = ((StableInvocationHandler<DomElement>) handler).getWrappedElement();
			return element == null ? null : getDomInvocationHandler(element);
		}
		if(handler instanceof DomInvocationHandler)
		{
			return (DomInvocationHandler) handler;
		}
		return null;
	}

	@Nonnull
	public static DomInvocationHandler getNotNullHandler(DomElement proxy)
	{
		DomInvocationHandler handler = getDomInvocationHandler(proxy);
		if(handler == null)
		{
			throw new AssertionError("null handler for " + proxy);
		}
		return handler;
	}

	public static StableInvocationHandler getStableInvocationHandler(Object proxy)
	{
		return (StableInvocationHandler) InvocationHandlerOwner.getHandler(proxy);
	}

	public DomApplicationComponent getApplicationComponent()
	{
		return myApplicationComponent;
	}

	@Override
	public final Project getProject()
	{
		return myProject;
	}

	@Override
	@Nonnull
	public final <T extends DomElement> DomFileElementImpl<T> getFileElement(final XmlFile file, final Class<T> aClass, String rootTagName)
	{
		//noinspection unchecked
		if(file.getUserData(MOCK_DESCRIPTION) == null)
		{
			file.putUserData(MOCK_DESCRIPTION, new MockDomFileDescription<>(aClass, rootTagName, file));
			mySemService.clearCache();
		}
		final DomFileElementImpl<T> fileElement = getFileElement(file);
		assert fileElement != null;
		return fileElement;
	}


	@SuppressWarnings({"unchecked"})
	@Nonnull
	final <T extends DomElement> FileDescriptionCachedValueProvider<T> getOrCreateCachedValueProvider(final XmlFile xmlFile)
	{
		//noinspection ConstantConditions
		return mySemService.getSemElement(FILE_DESCRIPTION_KEY, xmlFile);
	}

	public final Set<DomFileDescription> getFileDescriptions(String rootTagName)
	{
		return myApplicationComponent.getFileDescriptions(rootTagName);
	}

	public final Set<DomFileDescription> getAcceptingOtherRootTagNameDescriptions()
	{
		return myApplicationComponent.getAcceptingOtherRootTagNameDescriptions();
	}

	@Nonnull
	@NonNls
	public final String getComponentName()
	{
		return getClass().getName();
	}

	final void runChange(Runnable change)
	{
		final boolean b = setChanging(true);
		try
		{
			change.run();
		}
		finally
		{
			setChanging(b);
		}
	}

	final boolean setChanging(final boolean changing)
	{
		boolean oldChanging = myChanging;
		if(changing)
		{
			assert !oldChanging;
		}
		myChanging = changing;
		return oldChanging;
	}

	@Override
	@Nullable
	public final <T extends DomElement> DomFileElementImpl<T> getFileElement(XmlFile file)
	{
		if(file == null)
		{
			return null;
		}
		if(!(file.getFileType() instanceof DomSupportEnabled))
		{
			return null;
		}
		final VirtualFile virtualFile = file.getVirtualFile();
		if(virtualFile != null && virtualFile.isDirectory())
		{
			return null;
		}
		return this.<T>getOrCreateCachedValueProvider(file).getFileElement();
	}

	@Nullable
	static <T extends DomElement> DomFileElementImpl<T> getCachedFileElement(@Nonnull XmlFile file)
	{
		//noinspection unchecked
		return SoftReference.dereference(file.getUserData(CACHED_FILE_ELEMENT));
	}

	@Override
	@Nullable
	public final <T extends DomElement> DomFileElementImpl<T> getFileElement(XmlFile file, Class<T> domClass)
	{
		final DomFileDescription description = getDomFileDescription(file);
		if(description != null && myApplicationComponent.assignabilityCache.isAssignable(domClass, description.getRootElementClass()))
		{
			return getFileElement(file);
		}
		return null;
	}

	@Override
	@Nullable
	public final DomElement getDomElement(final XmlTag element)
	{
		if(myChanging)
		{
			return null;
		}

		final DomInvocationHandler handler = getDomHandler(element);
		return handler != null ? handler.getProxy() : null;
	}

	@Override
	@Nullable
	public GenericAttributeValue getDomElement(final XmlAttribute attribute)
	{
		if(myChanging)
		{
			return null;
		}

		final AttributeChildInvocationHandler handler = mySemService.getSemElement(DOM_ATTRIBUTE_HANDLER_KEY, attribute);
		return handler == null ? null : (GenericAttributeValue) handler.getProxy();
	}

	@Nullable
	public DomInvocationHandler getDomHandler(final XmlElement tag)
	{
		if(tag == null)
		{
			return null;
		}

		List<DomInvocationHandler> cached = mySemService.getCachedSemElements(DOM_HANDLER_KEY, tag);
		if(cached != null && !cached.isEmpty())
		{
			return cached.get(0);
		}


		return mySemService.getSemElement(DOM_HANDLER_KEY, tag);
	}

	@Override
	@Nullable
	public AbstractDomChildrenDescription findChildrenDescription(@Nonnull final XmlTag tag, @Nonnull final DomElement parent)
	{
		return findChildrenDescription(tag, getDomInvocationHandler(parent));
	}

	static AbstractDomChildrenDescription findChildrenDescription(final XmlTag tag, final DomInvocationHandler parent)
	{
		final DomGenericInfoEx info = parent.getGenericInfo();
		return info.findChildrenDescription(parent, tag.getLocalName(), tag.getNamespace(), false, tag.getName());
	}

	public final boolean isDomFile(@Nullable PsiFile file)
	{
		return file instanceof XmlFile && getFileElement((XmlFile) file) != null;
	}

	@Nullable
	public final DomFileDescription<?> getDomFileDescription(PsiElement element)
	{
		if(element instanceof XmlElement)
		{
			final PsiFile psiFile = element.getContainingFile();
			if(psiFile instanceof XmlFile)
			{
				return getDomFileDescription((XmlFile) psiFile);
			}
		}
		return null;
	}

	@Override
	public final <T extends DomElement> T createMockElement(final Class<T> aClass, final Module module, final boolean physical)
	{
		final XmlFile file = (XmlFile) PsiFileFactory.getInstance(myProject).createFileFromText("a.xml", XmlFileType.INSTANCE, "", (long) 0, physical);
		file.putUserData(MOCK_ELEMENT_MODULE, module);
		file.putUserData(MOCK, new Object());
		return getFileElement(file, aClass, "I_sincerely_hope_that_nobody_will_have_such_a_root_tag_name").getRootElement();
	}

	@Override
	public final boolean isMockElement(DomElement element)
	{
		return DomUtil.getFile(element).getUserData(MOCK) != null;
	}

	@Override
	public final <T extends DomElement> T createStableValue(final Supplier<T> provider)
	{
		return createStableValue(provider, t -> t.isValid());
	}

	@Override
	public final <T> T createStableValue(final Supplier<T> provider, final Condition<T> validator)
	{
		final T initial = provider.get();
		assert initial != null;
		final StableInvocationHandler handler = new StableInvocationHandler<>(initial, provider, validator);

		final Set<Class> intf = new HashSet<>();
		ContainerUtil.addAll(intf, initial.getClass().getInterfaces());
		intf.add(StableElement.class);
		intf.add(InvocationHandlerOwner.class);

		//noinspection unchecked
		return (T) AdvancedProxyBuilder.create(initial.getClass().getSuperclass()).withInvocationHandler(handler).withInterfaces(intf.toArray(new Class[intf.size()])).build();
	}

	public final <T extends DomElement> void registerFileDescription(final DomFileDescription<T> description, Disposable parentDisposable)
	{
		registerFileDescription(description);
		Disposer.register(parentDisposable, new Disposable()
		{
			@Override
			public void dispose()
			{
				getFileDescriptions(description.getRootTagName()).remove(description);
				getAcceptingOtherRootTagNameDescriptions().remove(description);
			}
		});
	}

	public final void registerFileDescription(final DomFileDescription description)
	{
		mySemService.clearCache();

		myApplicationComponent.registerFileDescription(description);
	}

	@Override
	@Nonnull
	public final DomElement getResolvingScope(GenericDomValue element)
	{
		final DomFileDescription<?> description = DomUtil.getFileElement(element).getFileDescription();
		return description.getResolveScope(element);
	}

	@Override
	@Nullable
	public final DomElement getIdentityScope(DomElement element)
	{
		final DomFileDescription description = DomUtil.getFileElement(element).getFileDescription();
		return description.getIdentityScope(element);
	}

	@Override
	public TypeChooserManager getTypeChooserManager()
	{
		return myApplicationComponent.getTypeChooserManager();
	}

	public void performAtomicChange(@Nonnull Runnable change)
	{
		mySemService.performAtomicChange(change);
		if(!mySemService.isInsideAtomicChange())
		{
			incModificationCount();
		}
	}

	public SemService getSemService()
	{
		return mySemService;
	}
}
