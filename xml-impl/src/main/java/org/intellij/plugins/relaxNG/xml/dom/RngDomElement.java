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

package org.intellij.plugins.relaxNG.xml.dom;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.util.xml.Attribute;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.GenericAttributeValue;
import consulo.xml.util.xml.JavaNameStrategy;
import consulo.xml.util.xml.NameStrategy;
import org.intellij.plugins.relaxNG.model.CommonElement;
import jakarta.annotation.Nonnull;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 16.08.2007
 */
@NameStrategy(JavaNameStrategy.class)
public interface RngDomElement extends DomElement, CommonElement<XmlElement> {
  /**
   * Returns the value of the ns child.
   *
   * @return the value of the ns child.
   */
  @Nonnull
  @Attribute("ns")
  GenericAttributeValue<String> getNamespace();

  /**
   * Returns the value of the datatypeLibrary child.
   *
   * @return the value of the datatypeLibrary child.
   */
  @Nonnull
  GenericAttributeValue<String> getDatatypeLibrary();
}
