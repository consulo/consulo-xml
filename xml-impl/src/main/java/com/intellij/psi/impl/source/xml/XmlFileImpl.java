/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xhtml.XHTMLLanguage;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.impl.source.html.ScriptSupportUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import consulo.language.ast.IElementType;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;

/**
 * @author Mike
 */
public class XmlFileImpl extends PsiFileImpl implements XmlFile {
  public XmlFileImpl(FileViewProvider viewProvider, IElementType elementType) {
    super(elementType, elementType, viewProvider);
  }

  @Override
  public XmlDocument getDocument() {
    PsiElement child = getFirstChild();
    while (child != null) {
      if (child instanceof XmlDocument) {
        return (XmlDocument) child;
      }
      child = child.getNextSibling();
    }

    return null;
  }

  @Override
  public XmlTag getRootTag() {
    XmlDocument document = getDocument();
    return document == null ? null : document.getRootTag();
  }

  @Override
  public boolean processElements(PsiElementProcessor processor, PsiElement place) {
    final XmlDocument document = getDocument();
    return document == null || document.processElements(processor, place);
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof XmlElementVisitor) {
      ((XmlElementVisitor) visitor).visitXmlFile(this);
    } else {
      visitor.visitFile(this);
    }
  }

  public String toString() {
    return "XmlFile:" + getName();
  }

  @Override
  @Nonnull
  public FileType getFileType() {
    return getViewProvider().getFileType();
  }

  @Override
  public void clearCaches() {
    super.clearCaches();

    if (isWebFileType()) {
      ScriptSupportUtil.clearCaches(this);
    }
  }

  private boolean isWebFileType() {
    return getLanguage() == XHTMLLanguage.INSTANCE || getLanguage() == HTMLLanguage.INSTANCE;
  }

  @Override
  public boolean processDeclarations(@Nonnull PsiScopeProcessor processor, @Nonnull ResolveState state, PsiElement lastParent, @Nonnull PsiElement place) {
    return super.processDeclarations(processor, state, lastParent, place) && (!isWebFileType() || ScriptSupportUtil.processDeclarations(this, processor, state, lastParent, place));
  }

  @Nonnull
  @Override
  public GlobalSearchScope getFileResolveScope() {
    return GlobalSearchScope.allScope(getProject());
  }

  @Override
  public boolean ignoreReferencedElementAccessibility() {
    return true;
  }
}
