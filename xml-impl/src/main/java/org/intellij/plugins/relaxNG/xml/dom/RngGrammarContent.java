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

import consulo.xml.util.xml.SubTag;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * http://relaxng.org/ns/structure/1.0:grammar-content interface.
 */
public interface RngGrammarContent extends RngDomElement {

  /**
   * Returns the list of start children.
   *
   * @return the list of start children.
   */
  @Nonnull
  @SubTag("start")        
  RngStart getStartElement();

  /**
   * Returns the list of define children.
   *
   * @return the list of define children.
   */
  @Nonnull
  List<RngDefine> getDefines();

  /**
   * Adds new child to the list of define children.
   *
   * @return created child
   */
  RngDefine addDefine();


  /**
   * Returns the list of div children.
   *
   * @return the list of div children.
   */
  @Nonnull
  List<RngDiv> getDivs();

  /**
   * Adds new child to the list of div children.
   *
   * @return created child
   */
  RngDiv addDiv();


  /**
   * Returns the list of include children.
   *
   * @return the list of include children.
   */
  @Nonnull
  List<RngInclude> getIncludes();

  /**
   * Adds new child to the list of include children.
   *
   * @return created child
   */
  RngInclude addInclude();
}
