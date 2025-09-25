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
package org.intellij.plugins.relaxNG.compact.psi.impl;

import consulo.language.ast.ASTNode;
import org.intellij.plugins.relaxNG.compact.psi.RncDiv;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import jakarta.annotation.Nonnull;

/**
 * @author sweinreuter
 * @since 2007-08-24
 */
public class RncDivImpl extends RncElementImpl implements RncDiv {
  public RncDivImpl(ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@Nonnull RncElementVisitor visitor) {
    visitor.visitDiv(this);
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visitDiv(this);
  }
}