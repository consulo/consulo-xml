/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.language.psi.PsiElement;
import consulo.xml.util.xml.GenericAttributeValue;
import org.intellij.plugins.relaxNG.model.Pattern;
import org.intellij.plugins.relaxNG.xml.dom.RngDefine;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 19.08.2007
 */
public abstract class RngDefineImpl extends RngDomElementBase implements RngDefine {
  @Override
  public void setName(String name) {
    final GenericAttributeValue<String> value = getNameAttr();
    if (value.getStringValue() != null) {
      value.setStringValue(name);
    }
  }

  @Override
  public String getName() {
    return getNameAttr().getValue();
  }

  @Override
  public PsiElement getNameElement() {
    return getNameAttr().getXmlAttributeValue();
  }

  @Override
  public Pattern getPattern() {
    return getPatternFrom(this);
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visitDefine(this);
  }
}
