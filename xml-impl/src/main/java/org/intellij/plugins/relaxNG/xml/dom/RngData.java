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

// Generated on Fri Aug 10 16:21:20 CEST 2007
// DTD/Schema  :    http://relaxng.org/ns/structure/1.0

package org.intellij.plugins.relaxNG.xml.dom;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.util.xml.GenericAttributeValue;
import consulo.xml.util.xml.Required;
import org.intellij.plugins.relaxNG.model.Pattern;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * http://relaxng.org/ns/structure/1.0:dataElemType interface.
 */
public interface RngData extends RngDomElement, Pattern<XmlElement> {

  /**
   * Returns the value of the type child.
   *
   * @return the value of the type child.
   */
  @Nonnull
  @Required
  GenericAttributeValue<String> getType();

  /**
   * Returns the list of param children.
   *
   * @return the list of param children.
   */
  @Nonnull
  List<RngParam> getParams();

  /**
   * Adds new child to the list of param children.
   *
   * @return created child
   */
  RngParam addParam();


  /**
   * Returns the list of except children.
   *
   * @return the list of except children.
   */
  @Nonnull
  RngExcept getExcept();
}
