/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.xml.util.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.util.CompositeModificationTracker;
import consulo.component.util.ModificationTracker;
import consulo.disposer.Disposable;
import consulo.ide.ServiceManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.NotNullLazyKey;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.highlighting.DomElementAnnotationHolder;
import consulo.xml.util.xml.highlighting.DomHighlightingHelper;
import consulo.xml.util.xml.reflect.AbstractDomChildrenDescription;
import consulo.xml.util.xml.reflect.DomGenericInfo;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * @author peter
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class DomManager extends CompositeModificationTracker implements ModificationTracker
{
	public static final Key<Module> MOCK_ELEMENT_MODULE = Key.create("MockElementModule");

	private final static NotNullLazyKey<DomManager, Project> INSTANCE_CACHE = ServiceManager.createLazyKey(DomManager.class);

	public static DomManager getDomManager(Project project)
	{
		return INSTANCE_CACHE.getValue(project);
	}

	public DomManager(@Nonnull Project project)
	{
		super(PsiManager.getInstance(project).getModificationTracker().getOutOfCodeBlockModificationTracker());
	}

	public abstract Project getProject();

	/**
	 * @param file     XML file
	 * @param domClass desired DOM element class
	 * @return New or cached DOM file element for the given file. All registered {@link DomFileDescription}s are
	 * asked if they are responsible for the file {@link DomFileDescription#isMyFile(XmlFile)}.
	 * If there is a {@link DomFileDescription} that is responsible for the file, but its {@link DomFileDescription#getRootElementClass()}
	 * result is incompatible with domClass parameter, null is returned
	 */
	@Nullable
	public abstract <T extends DomElement> DomFileElement<T> getFileElement(XmlFile file, Class<T> domClass);

	@Nullable
	@Deprecated
	/**
	 * @deprecated use {@link #getFileElement(XmlFile, Class)}
	 */ public abstract <T extends DomElement> DomFileElement<T> getFileElement(XmlFile file);

	@Nonnull
	@Deprecated
	/**
	 * @deprecated use {@link #getFileElement(XmlFile, Class)}
	 */ public abstract <T extends DomElement> DomFileElement<T> getFileElement(XmlFile file, Class<T> aClass, @NonNls String rootTagName);

	public abstract void addDomEventListener(DomEventListener listener, Disposable parentDisposable);

	/**
	 * @param type Type. Only {@link Class} and {@link ParameterizedType} are allowed
	 * @return {@link DomGenericInfo} instance for the desired type
	 */
	public abstract DomGenericInfo getGenericInfo(Type type);

	/**
	 * @param element tag
	 * @return DOM element for the given tag. If DOM isn't initialized for the containing file, it will be initialized
	 */
	@Nullable
	public abstract DomElement getDomElement(@Nullable final XmlTag element);

	/**
	 * @param element attribute
	 * @return DOM element for the given XML attribute. If DOM isn't initialized for the containing file, it will be initialized
	 */
	@Nullable
	public abstract GenericAttributeValue getDomElement(final XmlAttribute element);

	/**
	 * @param aClass   Desired DOM element class
	 * @param module   One may wish the result to think that it is in a particular module
	 * @param physical see {@link PsiFile#isPhysical()}
	 * @return DOM element which doesn't have any real file under itself. A mock file is created for it. See
	 * {@link PsiFileFactory#createFileFromText(String, FileType, CharSequence, long, boolean, boolean)}
	 */
	public abstract <T extends DomElement> T createMockElement(Class<T> aClass, final Module module, final boolean physical);

	/**
	 * @param element DOM element
	 * @return true if this element was created by {@link #createMockElement(Class, Module, boolean)} method
	 */
	public abstract boolean isMockElement(DomElement element);

	/**
	 * Creates DOM element of needed type, that is wrapper around real DOM element. Once the wrapped element
	 * becomes invalid, a new value is requested from provider parameter, so there's a possibility to
	 * restore the functionality. The resulting element will also implement StableElement interface.
	 *
	 * @param provider provides values to be wrapped
	 * @return stable DOM element
	 */
	public abstract <T extends DomElement> T createStableValue(Supplier<T> provider);

	public abstract <T> T createStableValue(final Supplier<T> provider, final Condition<T> validator);

	/**
	 * @return {@link ConverterManager} instance
	 * @deprecated This will be moved at the application level
	 */
	public abstract ConverterManager getConverterManager();

	public abstract ModelMerger createModelMerger();


	/**
	 * @param element reference element
	 * @return element that represents the resolve scope for the given reference. {@link DomResolveConverter} uses
	 * this method to resolve DOM references. This result's subtree will be traversed recursively searching for the reference target. See
	 * {@link Resolve} annotation.
	 */
	@Nonnull
	public abstract DomElement getResolvingScope(GenericDomValue element);

	/**
	 * @param element Named DOM element
	 * @return The scope within which the element's name identity will be checked by
	 * {@link DomHighlightingHelper#checkNameIdentity(DomElement, DomElementAnnotationHolder)}
	 */
	@Nullable
	public abstract DomElement getIdentityScope(DomElement element);

	/**
	 * @return {@link TypeChooserManager} instance
	 */
	public abstract TypeChooserManager getTypeChooserManager();

	@Nullable
	public abstract AbstractDomChildrenDescription findChildrenDescription(@Nonnull XmlTag templateChildTag, @Nonnull DomElement parent);

	@Nullable
	public final DomFileDescription<?> getDomFileDescription(final XmlFile xmlFile)
	{
		final DomFileElement<DomElement> element = getFileElement(xmlFile);
		return element != null ? element.getFileDescription() : null;
	}
}
