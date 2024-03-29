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
package consulo.xml.psi.impl.source.html.dtd;

import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlElement;
import consulo.util.collection.ArrayUtil;
import com.intellij.xml.XmlAttributeDescriptor;

/**
 * @author Maxim.Mossienko
 */
public class HtmlAttributeDescriptorImpl implements XmlAttributeDescriptor {
  private final XmlAttributeDescriptor delegate;
  private final boolean myCaseSensitive;

  public HtmlAttributeDescriptorImpl(XmlAttributeDescriptor _delegate, boolean caseSensitive) {
    delegate = _delegate;
    myCaseSensitive = caseSensitive;
  }

  public boolean isRequired() {
    return delegate.isRequired();
  }

  public boolean isFixed() {
    return delegate.isFixed();
  }

  public boolean hasIdType() {
    return delegate.hasIdType();
  }

  public boolean hasIdRefType() {
    return delegate.hasIdRefType();
  }

  public String getDefaultValue() {
    return delegate.getDefaultValue();
  }

  //todo: refactor to hierarchy of value descriptor?
  public boolean isEnumerated() {
    return delegate.isEnumerated();
  }

  public String[] getEnumeratedValues() {
    return delegate.getEnumeratedValues();
  }

  public String validateValue(XmlElement context, String value) {
    if (!myCaseSensitive) value = value.toLowerCase();
    return delegate.validateValue(context, value);
  }

  public PsiElement getDeclaration() {
    return delegate.getDeclaration();
  }

  public String getName(PsiElement context) {
    return delegate.getName(context);
  }

  public String getName() {
    return delegate.getName();
  }

  public void init(PsiElement element) {
    delegate.init(element);
  }

  public Object[] getDependences() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }
}
