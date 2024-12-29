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

package org.intellij.plugins.relaxNG.compact.formatting;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.ChildAttributes;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Spacing;
import consulo.language.psi.PsiElement;
import consulo.language.codeStyle.Wrap;
import org.intellij.plugins.relaxNG.compact.RncElementTypes;
import org.intellij.plugins.relaxNG.compact.RncTokenTypes;
import org.intellij.plugins.relaxNG.compact.psi.RncDecl;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncFile;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;
import org.intellij.plugins.relaxNG.compact.psi.impl.RncDocument;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class RncBlock implements Block {
  private final ASTNode myNode;

  public RncBlock(ASTNode element) {
    myNode = element;
  }

  @Override
  @Nonnull
  public TextRange getTextRange() {
    return myNode.getTextRange();
  }

  @Override
  @Nonnull
  public List<Block> getSubBlocks() {
    final List<Block> list = new ArrayList<>();
    ASTNode node = myNode.getFirstChildNode();
    while (node != null) {
      if (!RncTokenTypes.WHITESPACE.contains(node.getElementType()) && node.getTextLength() > 0) {
        list.add(new RncBlock(node));
      }
      node = node.getTreeNext();
    }
    return list;
  }

  @Override
  @Nullable
  public Wrap getWrap() {
    // TODO
    return null;
  }

  @Override
  @Nullable
  public Indent getIndent() {
    if (myNode.getTreeParent() == null) return null;
    if (isTopLevel()) {
      return Indent.getAbsoluteNoneIndent();
    } else if (myNode.getTreeParent().getPsi() instanceof RncGrammar && !RncTokenTypes.BRACES.contains(myNode.getElementType())) {
      // TODO: fix block psi
      return Indent.getNormalIndent();
    }
    return null;
  }

  private boolean isTopLevel() {
    final PsiElement parent = myNode.getTreeParent().getPsi();
    return parent instanceof RncDocument ||
            parent instanceof RncFile ||
            parent instanceof RncGrammar && parent.getParent() instanceof RncDocument;
  }

  @Override
  @Nullable
  public Alignment getAlignment() {
    // TODO
    return null;
  }

  @Override
  @Nullable
  public Spacing getSpacing(Block child1, @Nonnull Block child2) {
    if (child1 == null) {
      return null;
    }
    final ASTNode lnode = ((RncBlock)child1).myNode;
    final PsiElement lpsi = lnode.getPsi();
    final PsiElement rpsi = ((RncBlock)child2).myNode.getPsi();

    if (lpsi instanceof RncDecl && rpsi instanceof RncDecl) {
      return makeNewline();
    }
    if ((lpsi instanceof RncDecl || lpsi instanceof RncDefine ||
         lnode.getElementType() == RncElementTypes.START) &&
        (rpsi instanceof RncDefine || rpsi instanceof RncGrammar))
    {
      return makeNewline();
    }
    return null;
  }

  private static Spacing makeNewline() {
    return Spacing.createSpacing(0, Integer.MAX_VALUE, 1, true, 100);
  }

  @Override
  @Nonnull
  public ChildAttributes getChildAttributes(int newChildIndex) {
    return new ChildAttributes(null, null);
  }

  @Override
  public boolean isIncomplete() {
    // TODO
    return false;
  }

  @Override
  public boolean isLeaf() {
    // TODO
    return false;
  }
}