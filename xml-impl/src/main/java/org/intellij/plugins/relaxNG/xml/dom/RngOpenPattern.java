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


import consulo.xml.language.psi.XmlElement;
import consulo.xml.dom.DomElement;
import consulo.xml.dom.SubTag;
import org.intellij.plugins.relaxNG.model.Pattern;

/**
 * http://relaxng.org/ns/structure/1.0:open-pattern interface.
 */
@SuppressWarnings("UnusedDeclaration")
public interface RngOpenPattern extends DomElement, Pattern<XmlElement> {

  /**
   * Returns the value of the element child.
   *
   * @return the value of the element child.
   */
  RngElement getElement();


  /**
   * Returns the value of the attribute child.
   *
   * @return the value of the attribute child.
   */
  RngAttribute getAttribute();


  /**
   * Returns the value of the group child.
   *
   * @return the value of the group child.
   */
  RngGroup getGroup();


  /**
   * Returns the value of the interleave child.
   *
   * @return the value of the interleave child.
   */
  @SubTag(value = "interleave")
  RngInterleave getInterleave();


  /**
   * Returns the value of the choice child.
   *
   * @return the value of the choice child.
   */
  RngChoice getChoice();


  /**
   * Returns the value of the optional child.
   *
   * @return the value of the optional child.
   */
  RngOptional getOptional();


  /**
   * Returns the value of the zeroOrMore child.
   *
   * @return the value of the zeroOrMore child.
   */
  RngZeroOrMore getZeroOrMore();


  /**
   * Returns the value of the oneOrMore child.
   *
   * @return the value of the oneOrMore child.
   */
  RngOneOrMore getOneOrMore();


  /**
   * Returns the value of the list child.
   *
   * @return the value of the list child.
   */
  RngList getList();


  /**
   * Returns the value of the mixed child.
   *
   * @return the value of the mixed child.
   */
  RngMixed getMixed();


  /**
   * Returns the value of the ref child.
   *
   * @return the value of the ref child.
   */
  RngRef getRef();


  /**
   * Returns the value of the parentRef child.
   *
   * @return the value of the parentRef child.
   */
  RngParentRef getParentRef();


  /**
   * Returns the value of the empty child.
   *
   * @return the value of the empty child.
   */
  RngEmpty getEmpty();


  /**
   * Returns the value of the text child.
   *
   * @return the value of the text child.
   */
  RngText getText();


  /**
   * Returns the value of the value child.
   *
   * @return the value of the value child.
   */
  RngValue getValue();


  /**
   * Returns the value of the data child.
   *
   * @return the value of the data child.
   */
  RngData getData();


  /**
   * Returns the value of the notAllowed child.
   *
   * @return the value of the notAllowed child.
   */
  RngNotAllowed getNotAllowed();


  /**
   * Returns the value of the externalRef child.
   *
   * @return the value of the externalRef child.
   */
  RngExternalRef getExternalRef();


  /**
   * Returns the value of the grammar child.
   *
   * @return the value of the grammar child.
   */
  RngGrammar getGrammar();
}
