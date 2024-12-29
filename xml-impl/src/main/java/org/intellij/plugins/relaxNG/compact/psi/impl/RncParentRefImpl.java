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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.PsiFile;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;
import org.intellij.plugins.relaxNG.compact.psi.RncParentRef;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 11.08.2007
 */
public class RncParentRefImpl extends RncRefImpl implements RncParentRef {
  public RncParentRefImpl(ASTNode node) {
    super(node);
  }

  @Override // super asserts nameNode != null
  public String getReferencedName() {
    final ASTNode node = findNameNode();
    return node == null ? null : super.getReferencedName();
  }

  @Override
  public void accept(@Nonnull RncElementVisitor visitor) {
    visitor.visitParentRef(this);
  }

  @Override
  public int getTextOffset() {
    final ASTNode astNode = findNameNode();
    return astNode != null ? astNode.getStartOffset() : getTextRange().getStartOffset();
  }

  @Override
  @Nullable
  public PsiReference getReference() {
    return new PatternReference(this) {
      @Override
      protected RncGrammar getScope() {
        final PsiElement scope = PsiTreeUtil.getParentOfType(getElement(), RncGrammar.class, true, PsiFile.class);
        if (scope == null) {
          return null;
        }
        return PsiTreeUtil.getParentOfType(scope, RncGrammar.class, true, PsiFile.class);
      }
    };
  }
}