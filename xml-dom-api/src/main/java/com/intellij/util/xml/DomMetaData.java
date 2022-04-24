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
package com.intellij.util.xml;

import com.intellij.psi.xml.XmlTag;
import consulo.language.psi.PsiElement;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.meta.PsiWritableMetaData;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;

/**
 * @author peter
 */
public class DomMetaData<T extends DomElement> implements PsiWritableMetaData, PsiPresentableMetaData, PsiMetaData {
  private T myElement;
  @Nullable
  private GenericDomValue myNameElement;

  public final PsiElement getDeclaration() {
    return myElement.getXmlTag();
  }

  public T getElement() {
    return myElement;
  }

  @NonNls
  public String getName(PsiElement context) {
    return getName();
  }

  @NonNls
  public final String getName() {
    final String s = ElementPresentationManager.getElementName(myElement);
    if (s != null) return s;

    final GenericDomValue value = getNameElement(myElement);
    return value == null ? null : value.getStringValue();
  }

  public void init(PsiElement element) {
    myElement = (T) DomManager.getDomManager(element.getProject()).getDomElement((XmlTag)element);
    assert myElement != null : element;
    myNameElement = getNameElement(myElement);
  }

  public void setElement(final T element) {
    myElement = element;
  }

  @Nullable
  protected GenericDomValue getNameElement(final T t) {
    return myElement.getGenericInfo().getNameDomElement(t);
  }

  public Object[] getDependences() {
    final PsiElement declaration = getDeclaration();
    if (myElement != null && myElement.isValid()) {
      return new Object[]{DomUtil.getRoot(myElement), declaration};
    }
    return new Object[]{declaration};
  }

  public void setName(String name) throws IncorrectOperationException {
    if (myNameElement != null) {
      myNameElement.setStringValue(name);
    }
  }

  public String getTypeName() {
    return ElementPresentationManager.getTypeNameForObject(myElement);
  }

  public Image getIcon() {
    return ElementPresentationManager.getIcon(myElement);
  }
}
