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

import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.util.dataholder.UserDataHolder;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.reflect.AbstractDomChildrenDescription;
import consulo.xml.util.xml.reflect.DomGenericInfo;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Type;

/**
 * Base interface for DOM elements. Every DOM interface should extend this one.
 *
 * @author peter
 */
public interface DomElement extends AnnotatedElement, UserDataHolder
{
  DomElement[] EMPTY_ARRAY = new DomElement[0];

  XmlTag getXmlTag();

  /**
   * Returns the underlying XML element/file.
   *
   * @return {@link XmlFile}, {@link XmlTag} or {@link XmlAttribute}
   */
  @Nullable
  XmlElement getXmlElement();

  DomElement getParent();

  XmlTag ensureTagExists();

  XmlElement ensureXmlElementExists();

  /**
   * Removes all corresponding XML content. In case of being collection member, invalidates the element.
   */
  void undefine();

  boolean isValid();

  /**
   * @return true if corresponding XML element exists
   */
  boolean exists();

  @Nonnull
  DomGenericInfo getGenericInfo();

  @Nonnull
  @NonNls String getXmlElementName();

  @Nonnull
  @NonNls String getXmlElementNamespace();

  /**
   * @return namespace key if this element or one of its ancestors is annotated with
   * {@link Namespace}, or null otherwise, which means that namespace should be equal
   * to that of the element's parent
   */
  @Nullable
  @NonNls String getXmlElementNamespaceKey();

  void accept(final DomElementVisitor visitor);

  void acceptChildren(DomElementVisitor visitor);

  @Nonnull
  DomManager getManager();

  @Nonnull
  Type getDomElementType();

  AbstractDomChildrenDescription getChildDescription();

  @Nonnull
  DomNameStrategy getNameStrategy();

  @Nonnull
  ElementPresentation getPresentation();

  GlobalSearchScope getResolveScope();

  /**
   * Walk up the DOM tree searching for element of requiredClass type
   * @param requiredClass parent element's type
   * @param strict
   * <ul>
   * <li>strict = false: if the current element is already of the correct type, then it is returned.</li>
   * <li>strict = true: the returned element must be higher in the hierarchy.</li>
   * </ul> 
   * @return the parent of requiredClass type
   */
  @Nullable
  <T extends DomElement> T getParentOfType(Class<T> requiredClass, boolean strict);

  @Nullable
  Module getModule();

  void copyFrom(DomElement other);

  <T extends DomElement> T createMockCopy(final boolean physical);

  /**
   * @return stable element (see {@link DomManager#createStableValue(java.util.function.Supplier)}}),
   * that holds the complete 'XPath' to this element in XML. If this element is in collection, and something
   * is inserted before it, the stable copy behaviour may be unexpected. So use this method only when you
   * are sure that nothing serious will happen during the lifetime of the stable copy. The most usual use
   * case is when one creates something inside {@link WriteCommandAction} and
   * wants to use it outside the action. Due to formatting done on the command finish the element may become
   * invalidated, but the stable copy will survive, because nothing in fact has changed in its 'XPath'.
   */
  <T extends DomElement> T createStableCopy();

}
