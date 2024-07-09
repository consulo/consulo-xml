/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.xml.util;

import com.intellij.xml.impl.XmlEnumerationDescriptor;
import consulo.document.util.TextRange;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 *         Date: 16.08.13
 */
public class XmlEnumeratedValueReference extends PsiReferenceBase<XmlElement> implements EmptyResolveMessageProvider {
  private final XmlEnumerationDescriptor myDescriptor;

  public XmlEnumeratedValueReference(XmlElement value, XmlEnumerationDescriptor descriptor) {
    super(value);
    myDescriptor = descriptor;
  }

  public XmlEnumeratedValueReference(XmlElement value, XmlEnumerationDescriptor descriptor, TextRange range) {
    super(value, range);
    myDescriptor = descriptor;
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    return myDescriptor.getValueDeclaration(getElement(), getValue());
  }

  @Nonnull
  @Override
  public Object[] getVariants() {
    if (myDescriptor.isFixed()) {
      String defaultValue = myDescriptor.getDefaultValue();
      return defaultValue == null ? ArrayUtil.EMPTY_OBJECT_ARRAY : new Object[] {defaultValue};
    }
    else {
      String[] values = myDescriptor.getEnumeratedValues();
      return values == null ? ArrayUtil.EMPTY_OBJECT_ARRAY : values;
    }
  }

  @Nonnull
  @Override
  public LocalizeValue buildUnresolvedMessage(@Nonnull String referenceText) {
    String name = getElement() instanceof XmlTag ? "tag" : "attribute";
    return myDescriptor.isFixed()
      ? XmlErrorLocalize.shouldHaveFixedValue(StringUtil.capitalize(name), myDescriptor.getDefaultValue())
      : XmlErrorLocalize.wrongValue(name);
  }
}
