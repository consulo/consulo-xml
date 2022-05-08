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

import javax.annotation.Nullable;

import consulo.language.util.IncorrectOperationException;
import org.intellij.plugins.relaxNG.xml.dom.RngDefine;
import consulo.application.AllIcons;
import consulo.xml.util.xml.DomMetaData;
import consulo.xml.util.xml.GenericAttributeValue;
import consulo.xml.util.xml.GenericDomValue;
import consulo.ui.image.Image;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 19.08.2007
 */
public class RngDefineMetaData extends DomMetaData<RngDefine> {

  @Override
  @Nullable
  protected GenericDomValue getNameElement(final RngDefine element) {
    final GenericAttributeValue<String> id = element.getNameAttr();
    if (id.getXmlElement() != null) {
      return id;
    }
    return null;
  }

  @Override
  public void setName(final String name) throws IncorrectOperationException {
    getElement().setName(name);
  }

  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Property;
  }

  @Override
  public String getTypeName() {
    return "Pattern Definition";
  }
}
