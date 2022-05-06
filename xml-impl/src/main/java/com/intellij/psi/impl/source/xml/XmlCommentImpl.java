/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.impl.source.xml;

import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.impl.source.tree.injected.XmlCommentLiteralEscaper;
import com.intellij.psi.xml.*;
import consulo.language.ast.IElementType;
import consulo.language.psi.*;
import consulo.language.psi.meta.MetaDataService;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.util.PsiTreeUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Mike
 */
public class XmlCommentImpl extends XmlElementImpl implements XmlComment, XmlElementType, PsiMetaOwner, PsiLanguageInjectionHost {
  public XmlCommentImpl() {
    super(XML_COMMENT);
  }

  public IElementType getTokenType() {
    return XML_COMMENT;
  }

  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof XmlElementVisitor) {
      ((XmlElementVisitor)visitor).visitXmlComment(this);
    }
    else {
      visitor.visitComment(this);
    }
  }

  @Override
  public boolean isValidHost() {
    return true;
  }

  public XmlTag getParentTag() {
    if(getParent() instanceof XmlTag) return (XmlTag)getParent();
    return null;
  }

  public XmlTagChild getNextSiblingInTag() {
    if(getParent() instanceof XmlTag) return (XmlTagChild)getNextSibling();
    return null;
  }

  public XmlTagChild getPrevSiblingInTag() {
    if(getParent() instanceof XmlTag) return (XmlTagChild)getPrevSibling();
    return null;
  }

  @Nonnull
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this, XmlComment.class);
  }

  @Nullable
  public PsiMetaData getMetaData() {
    return MetaDataService.getInstance().getMeta(this);
  }

  public PsiLanguageInjectionHost updateText(@Nonnull final String text) {
    final PsiFile psiFile = getContainingFile();

    final XmlDocument document =
      ((XmlFile) PsiFileFactory.getInstance(getProject()).createFileFromText("dummy", psiFile.getFileType(), text)).getDocument();
    assert document != null;

    final XmlComment comment = PsiTreeUtil.getChildOfType(document, XmlComment.class);
    
    assert comment != null;
    replaceAllChildrenToChildrenOf(comment.getNode());

    return this;
  }

  @Nonnull
  public LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
    return new XmlCommentLiteralEscaper(this);
  }
}
