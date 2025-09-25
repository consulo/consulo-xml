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

import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.IncorrectOperationException;
import consulo.virtualFileSystem.fileType.FileType;
import org.intellij.plugins.relaxNG.compact.RncElementTypes;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.compact.RngCompactLanguage;
import org.intellij.plugins.relaxNG.compact.psi.RncDecl;
import org.intellij.plugins.relaxNG.compact.psi.RncFile;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;

import jakarta.annotation.Nonnull;

/**
 * @author sweinreuter
 * @since 2007-08-01
 */
public class RncFileImpl extends PsiFileBase implements RncFile, XmlFile {
  private static final TokenSet DECLS = TokenSet.create(RncElementTypes.NS_DECL, RncElementTypes.DATATYPES_DECL);

  public RncFileImpl(FileViewProvider viewProvider) {
    super(viewProvider, RngCompactLanguage.INSTANCE);
  }

  @Override
  @Nonnull
  public FileType getFileType() {
    return RncFileType.getInstance();
  }

  @Override
  @Nonnull
  public XmlDocument getDocument() {
    // this needs to be a seperate child element because of com.intellij.util.xml.impl.ExternalChangeProcessor.visitDocumentChanged()
    final XmlDocument document = findChildByClass(XmlDocument.class);
    assert document != null;
    return document;
  }

  @Override
  public XmlTag getRootTag() {
    return getDocument().getRootTag();
  }

  @Override
  public boolean processDeclarations(@Nonnull PsiScopeProcessor processor, @Nonnull ResolveState substitutor, PsiElement lastParent, @Nonnull PsiElement place) {
    //processor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, this);
    try {
      final ASTNode docNode = getDocument().getNode();
      assert docNode != null;
      final ASTNode[] nodes = docNode.getChildren(DECLS);
      for (ASTNode node : nodes) {
        if (!processor.execute(node.getPsi(), substitutor)) {
          return false;
        }
      }

      final RncGrammar grammar = getGrammar();
      if (grammar != null) {
        return grammar.processDeclarations(processor, substitutor, lastParent, place);
      } else {
        return true;
      }
    } finally {
      //processor.handleEvent(JavaScopeProcessorEvent.SET_CURRENT_FILE_CONTEXT, null);
    }
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    return getDocument().add(element);
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    return getDocument().addAfter(element, anchor);
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws consulo.language.util.IncorrectOperationException {
    return getDocument().addBefore(element, anchor);
  }

  @Override
  public boolean processElements(PsiElementProcessor processor, PsiElement place) {
    return false;
  }

  @Nonnull
  @Override
  public GlobalSearchScope getFileResolveScope() {
    return GlobalSearchScope.allScope(getProject());
  }

  @Override
  public boolean ignoreReferencedElementAccessibility() {
    return false;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ":" + getName();
  }

  @Override
  public RncDecl[] getDeclarations() {
    return ((RncDocument)getDocument()).findChildrenByClass(RncDecl.class);
  }

  @Override
  public RncGrammar getGrammar() {
    final XmlDocument document = getDocument();
    return ((RncDocument)document).getGrammar();
  }
}