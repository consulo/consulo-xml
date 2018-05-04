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

import com.intellij.icons.AllIcons;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomMetaData;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.intellij.plugins.relaxNG.xml.dom.RngDefine;

import javax.annotation.Nullable;
import javax.swing.*;

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
  public Icon getIcon() {
    return AllIcons.Nodes.Property;
  }

  @Override
  public String getTypeName() {
    return "Pattern Definition";
  }
}
