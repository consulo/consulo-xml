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
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.util.IncorrectOperationException;
import org.intellij.plugins.relaxNG.compact.RncElementTypes;
import org.intellij.plugins.relaxNG.compact.psi.RncElement;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.model.CommonElement;
import jakarta.annotation.Nonnull;

public abstract class RncElementImpl extends ASTWrapperPsiElement implements RncElement, CommonElement<RncElement> {
  private static final TokenSet GRAMMAR_CONTENT = TokenSet.create(
          RncElementTypes.START,
          RncElementTypes.DEFINE, RncElementTypes.DIV,
          RncElementTypes.GRAMMAR_PATTERN, RncElementTypes.PATTERN,
          RncElementTypes.INCLUDE
  );

  public RncElementImpl(ASTNode node) {
    super(node);
  }

  public String toString() {
    return getNode().getElementType().toString();
  }

  @Override
  public void delete() throws IncorrectOperationException {
    getNode().getTreeParent().removeChild(getNode());
  }

  @Override
  public PsiElement add(@Nonnull PsiElement psiElement) throws consulo.language.util.IncorrectOperationException {
    final ASTNode astNode = psiElement.getNode();
    assert astNode != null;

    getNode().addChild(astNode);
    final PsiElement r = getNode().getLastChildNode().getPsi();

    assert r.getClass() == psiElement.getClass();
    return r;
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement psiElement, PsiElement anchor) throws consulo.language.util.IncorrectOperationException {
    final ASTNode child = psiElement.getNode();
    assert child != null;

    final ASTNode anchorNode = anchor.getNode();
    assert anchorNode != null;

    getNode().addChild(child, anchorNode);
    final PsiElement r = anchorNode.getTreePrev().getPsi();

    assert r.getClass() == psiElement.getClass();
    return r;
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws consulo.language.util.IncorrectOperationException {
    final ASTNode astNode = anchor.getNode();
    assert astNode != null;

    final ASTNode node = astNode.getTreeNext();
    if (node == null) {
      return add(element);
    } else {
      return addBefore(element, node.getPsi());
    }
  }

  @Override
  public final void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof RncElementVisitor) {
      accept((RncElementVisitor)visitor);
    } else {
      visitor.visitElement(this);
    }
  }

  @Override
  public boolean processDeclarations(@Nonnull PsiScopeProcessor processor, @Nonnull ResolveState substitutor, PsiElement lastParent, @Nonnull PsiElement place) {
    final ASTNode astNode = getNode();

    final ASTNode[] children = astNode.getChildren(GRAMMAR_CONTENT);
    processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, this);
    for (ASTNode element : children) {
      if (!processor.execute(element.getPsi(), substitutor)) {
        return false;
      }
    }
    processor.handleEvent(PsiScopeProcessor.Event.SET_DECLARATION_HOLDER, null);
    return true;
  }

  public abstract void accept(@Nonnull RncElementVisitor visitor);

  @Override
  public void accept(Visitor visitor) {
    visitor.visitElement(this);
  }

  @Override
  public void acceptChildren(Visitor visitor) {
    final PsiElement[] elements = getChildren();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < elements.length; i++) {
      PsiElement element = elements[i];
      if (element instanceof CommonElement) {
        ((CommonElement)element).accept(visitor);
      }
    }
  }

  @Override
  public RncElement getPsiElement() {
    return this;
  }
}