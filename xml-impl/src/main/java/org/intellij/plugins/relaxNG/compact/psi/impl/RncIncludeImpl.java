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

import jakarta.annotation.Nonnull;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.PsiFile;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.RncInclude;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 10.08.2007
 */
public class RncIncludeImpl extends RncFileReferenceImpl implements RncInclude {
  public RncIncludeImpl(ASTNode node) {
    super(node);
  }

  @Override
  public boolean processDeclarations(@Nonnull PsiScopeProcessor processor, @Nonnull ResolveState substitutor, PsiElement lastParent, @Nonnull PsiElement place) {
    final PsiElement[] children = getChildren();
    for (PsiElement child : children) {
      if (!processor.execute(child, substitutor)) {
        return false;
      }
    }
    return super.processDeclarations(processor, substitutor, lastParent, place);
  }

  @Override
  public void accept(@Nonnull RncElementVisitor visitor) {
    visitor.visitInclude(this);
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visitInclude(this);
  }

  @Override
  public PsiFile getInclude() {
    return getReferencedFile();
  }

  @Override
  @Nonnull
  public RncDefine[] getOverrides() {
    // TODO: DIVs?
    return findChildrenByClass(RncDefine.class);
  }
}
