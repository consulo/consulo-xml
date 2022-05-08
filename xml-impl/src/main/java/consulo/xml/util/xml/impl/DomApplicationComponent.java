/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.component.extension.Extensions;
import consulo.ide.ServiceManager;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomElementVisitor;
import consulo.xml.util.xml.DomFileDescription;
import consulo.xml.util.xml.TypeChooserManager;
import consulo.xml.util.xml.highlighting.DomElementsAnnotator;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.ide.impl.idea.util.ReflectionAssignabilityCache;
import consulo.util.collection.FactoryMap;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static consulo.util.collection.ContainerUtil.newArrayList;

/**
 * @author peter
 */
@Singleton
public class DomApplicationComponent
{
	private final Map<String, Set<DomFileDescription>> myRootTagName2FileDescription = FactoryMap.create(k -> new HashSet<DomFileDescription>());
	private final Set<DomFileDescription> myAcceptingOtherRootTagNamesDescriptions = new HashSet<DomFileDescription>();
	private final ImplementationClassCache myCachedImplementationClasses = new ImplementationClassCache(DomImplementationClassEP.EP_NAME);
	private final TypeChooserManager myTypeChooserManager = new TypeChooserManager();
	final ReflectionAssignabilityCache assignabilityCache = new ReflectionAssignabilityCache();
	private final Map<Class, DomElementsAnnotator> myClass2Annotator = ConcurrentFactoryMap.createMap(key ->
	{
		final DomFileDescription desc = findFileDescription(key);
		return desc == null ? null : desc.createAnnotator();
	});

	private final SofterCache<Type, StaticGenericInfo> myGenericInfos = SofterCache.create(new NotNullFunction<Type, StaticGenericInfo>()
	{
		@Nonnull
		@Override
		public StaticGenericInfo fun(Type type)
		{
			return new StaticGenericInfo(type);
		}
	});
	private final SofterCache<Class, InvocationCache> myInvocationCaches = SofterCache.create(new NotNullFunction<Class, InvocationCache>()
	{
		@Nonnull
		@Override
		public InvocationCache fun(Class key)
		{
			return new InvocationCache(key);
		}
	});
	private final Map<Class<? extends DomElementVisitor>, VisitorDescription> myVisitorDescriptions = ConcurrentFactoryMap.createMap(key -> new VisitorDescription(key));

	public DomApplicationComponent()
	{
		for(final DomFileDescription description : Extensions.getExtensions(DomFileDescription.EP_NAME))
		{
			registerFileDescription(description);
		}
	}

	public static DomApplicationComponent getInstance()
	{
		return ServiceManager.getService(DomApplicationComponent.class);
	}

	public int getCumulativeVersion()
	{
		int result = 0;
		for(DomFileDescription description : getAllFileDescriptions())
		{
			result += description.getVersion();
			result += description.getRootTagName().hashCode(); // so that a plugin enabling/disabling could trigger the reindexing
		}
		return result;
	}

	public final synchronized Set<DomFileDescription> getFileDescriptions(String rootTagName)
	{
		return myRootTagName2FileDescription.get(rootTagName);
	}

	public final synchronized Set<DomFileDescription> getAcceptingOtherRootTagNameDescriptions()
	{
		return myAcceptingOtherRootTagNamesDescriptions;
	}

	public final synchronized void registerFileDescription(final DomFileDescription description)
	{
		myRootTagName2FileDescription.get(description.getRootTagName()).add(description);
		if(description.acceptsOtherRootTagNames())
		{
			myAcceptingOtherRootTagNamesDescriptions.add(description);
		}

		//noinspection unchecked
		final Map<Class<? extends DomElement>, Class<? extends DomElement>> implementations = description.getImplementations();
		for(final Map.Entry<Class<? extends DomElement>, Class<? extends DomElement>> entry : implementations.entrySet())
		{
			registerImplementation(entry.getKey(), entry.getValue(), null);
		}

		myTypeChooserManager.copyFrom(description.getTypeChooserManager());
	}

	public synchronized List<DomFileDescription> getAllFileDescriptions()
	{
		final List<DomFileDescription> result = newArrayList();
		for(Set<DomFileDescription> descriptions : myRootTagName2FileDescription.values())
		{
			result.addAll(descriptions);
		}
		result.addAll(myAcceptingOtherRootTagNamesDescriptions);
		return result;
	}

	@Nullable
	private synchronized DomFileDescription findFileDescription(Class rootElementClass)
	{
		for(Set<DomFileDescription> descriptions : myRootTagName2FileDescription.values())
		{
			for(DomFileDescription description : descriptions)
			{
				if(description.getRootElementClass() == rootElementClass)
				{
					return description;
				}
			}
		}

		for(DomFileDescription description : myAcceptingOtherRootTagNamesDescriptions)
		{
			if(description.getRootElementClass() == rootElementClass)
			{
				return description;
			}
		}
		return null;
	}

	public DomElementsAnnotator getAnnotator(Class rootElementClass)
	{
		return myClass2Annotator.get(rootElementClass);
	}

	@Nullable
	final Class<? extends DomElement> getImplementation(final Class concreteInterface)
	{
		//noinspection unchecked
		return myCachedImplementationClasses.get(concreteInterface);
	}

	public final void registerImplementation(Class<? extends DomElement> domElementClass, Class<? extends DomElement> implementationClass,
											 @Nullable final Disposable parentDisposable)
	{
		myCachedImplementationClasses.registerImplementation(domElementClass, implementationClass, parentDisposable);
	}

	public TypeChooserManager getTypeChooserManager()
	{
		return myTypeChooserManager;
	}

	public final StaticGenericInfo getStaticGenericInfo(final Type type)
	{
		return myGenericInfos.getCachedValue(type);
	}

	final InvocationCache getInvocationCache(final Class type)
	{
		return myInvocationCaches.getCachedValue(type);
	}

	public final VisitorDescription getVisitorDescription(Class<? extends DomElementVisitor> aClass)
	{
		return myVisitorDescriptions.get(aClass);
	}

}
