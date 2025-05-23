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

import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.document.util.TextRange;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.util.collection.ArrayUtil;
import consulo.language.util.IncorrectOperationException;
import org.intellij.plugins.relaxNG.compact.RncTokenTypes;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.RncFile;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;
import org.intellij.plugins.relaxNG.compact.psi.RncInclude;
import org.intellij.plugins.relaxNG.compact.psi.RncPattern;
import org.intellij.plugins.relaxNG.compact.psi.util.EscapeUtil;
import org.intellij.plugins.relaxNG.compact.psi.util.RenameUtil;
import org.intellij.plugins.relaxNG.model.Define;
import org.intellij.plugins.relaxNG.model.resolve.DefinitionResolver;
import org.jetbrains.annotations.NonNls;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.ui.image.Image;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 13.08.2007
 */
public class RncDefineImpl extends RncElementImpl implements RncDefine, PsiMetaOwner {
  public RncDefineImpl(ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@Nonnull RncElementVisitor visitor) {
    visitor.visitDefine(this);
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visitDefine(this);
  }

  @Override
  public String getName() {
    final ASTNode node = getNameNode();
    return EscapeUtil.unescapeText(node);
  }

  @Override
  public PsiElement getNameElement() {
    return getNameNode().getPsi();
  }

  @Nonnull
  public ASTNode getNameNode() {
    final ASTNode node = getNode().findChildByType(RncTokenTypes.IDENTIFIERS);
    assert node != null;
    return node;
  }

  @Override
  public PsiElement setName(@NonNls @Nonnull String name) throws IncorrectOperationException {
    final ASTNode node = getNameNode();
    node.getTreeParent().replaceChild(node, RenameUtil.createIdentifierNode(getManager(), name));
    return this;
  }

  @Override
  @Nullable
  public RncPattern getPattern() {
    return findChildByClass(RncPattern.class);
  }

  @Override
  public PsiReference getReference() {
    if (getParent() instanceof RncInclude) {
      final TextRange range = TextRange.from(0, getNameNode().getTextLength());
      return new PsiReferenceBase<RncDefine>(this, range, true) {
        @Override
        public PsiElement resolve() {
          return RncDefineImpl.this;
        }

        @Override
        @Nonnull
        public Object[] getVariants() {
          final RncInclude parent = (RncInclude)getParent();
          final RncFile referencedFile = parent.getReferencedFile();
          if (referencedFile == null) {
            return EMPTY_ARRAY;
          }
          final RncGrammar grammar = referencedFile.getGrammar();
          if (grammar == null) {
            return EMPTY_ARRAY;
          }

          final Map<String, Set<Define>> map = DefinitionResolver.getAllVariants(grammar);
          if (map != null) {
            return map.keySet().toArray();
          }
          return EMPTY_ARRAY;
        }
      };
    }
    return super.getReference();
  }

  public boolean isMetaEnough() {
    return true;
  }

  @Override
  @Nullable
  public PsiMetaData getMetaData() {
    return new MyMetaData();
  }

  private class MyMetaData implements PsiMetaData, PsiPresentableMetaData {
    /*public boolean processDeclarations(PsiElement context, PsiScopeProcessor processor, PsiSubstitutor substitutor, PsiElement lastElement, PsiElement place) {
      return false;
    }*/

    @Override
    @Nullable
    public Image getIcon() {
      return IconDescriptorUpdaters.getIcon(RncDefineImpl.this, 0);
    }

    @Override
    public String getTypeName() {
      return "Pattern Definition";
    }

    @Override
    public PsiElement getDeclaration() {
      return RncDefineImpl.this;
    }

    @Override
    @NonNls
    public String getName(PsiElement context) {
      return RncDefineImpl.this.getName();
    }

    @Override
    @NonNls
    public String getName() {
      return RncDefineImpl.this.getName();
    }

    @Override
    public void init(PsiElement element) {
    }

    @Override
    public Object[] getDependences() {
      return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }
  }
}
