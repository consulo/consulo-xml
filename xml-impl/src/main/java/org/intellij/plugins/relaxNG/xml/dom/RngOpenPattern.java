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

import javax.annotation.Nonnull;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.SubTag;
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
  @Nonnull
  RngElement getElement();


  /**
   * Returns the value of the attribute child.
   *
   * @return the value of the attribute child.
   */
  @Nonnull
  RngAttribute getAttribute();


  /**
   * Returns the value of the group child.
   *
   * @return the value of the group child.
   */
  @Nonnull
  RngGroup getGroup();


  /**
   * Returns the value of the interleave child.
   *
   * @return the value of the interleave child.
   */
  @Nonnull
  @SubTag(value = "interleave")
  RngInterleave getInterleave();


  /**
   * Returns the value of the choice child.
   *
   * @return the value of the choice child.
   */
  @Nonnull
  RngChoice getChoice();


  /**
   * Returns the value of the optional child.
   *
   * @return the value of the optional child.
   */
  @Nonnull
  RngOptional getOptional();


  /**
   * Returns the value of the zeroOrMore child.
   *
   * @return the value of the zeroOrMore child.
   */
  @Nonnull
  RngZeroOrMore getZeroOrMore();


  /**
   * Returns the value of the oneOrMore child.
   *
   * @return the value of the oneOrMore child.
   */
  @Nonnull
  RngOneOrMore getOneOrMore();


  /**
   * Returns the value of the list child.
   *
   * @return the value of the list child.
   */
  @Nonnull
  RngList getList();


  /**
   * Returns the value of the mixed child.
   *
   * @return the value of the mixed child.
   */
  @Nonnull
  RngMixed getMixed();


  /**
   * Returns the value of the ref child.
   *
   * @return the value of the ref child.
   */
  @Nonnull
  RngRef getRef();


  /**
   * Returns the value of the parentRef child.
   *
   * @return the value of the parentRef child.
   */
  @Nonnull
  RngParentRef getParentRef();


  /**
   * Returns the value of the empty child.
   *
   * @return the value of the empty child.
   */
  @Nonnull
  RngEmpty getEmpty();


  /**
   * Returns the value of the text child.
   *
   * @return the value of the text child.
   */
  @Nonnull
  RngText getText();


  /**
   * Returns the value of the value child.
   *
   * @return the value of the value child.
   */
  @Nonnull
  RngValue getValue();


  /**
   * Returns the value of the data child.
   *
   * @return the value of the data child.
   */
  @Nonnull
  RngData getData();


  /**
   * Returns the value of the notAllowed child.
   *
   * @return the value of the notAllowed child.
   */
  @Nonnull
  RngNotAllowed getNotAllowed();


  /**
   * Returns the value of the externalRef child.
   *
   * @return the value of the externalRef child.
   */
  @Nonnull
  RngExternalRef getExternalRef();


  /**
   * Returns the value of the grammar child.
   *
   * @return the value of the grammar child.
   */
  @Nonnull
  RngGrammar getGrammar();
}
