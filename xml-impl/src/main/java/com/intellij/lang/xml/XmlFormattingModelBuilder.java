/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.lang.xml;

import com.intellij.psi.formatter.xml.XmlBlock;
import com.intellij.psi.formatter.xml.XmlPolicy;
import consulo.document.util.TextRange;
import consulo.ide.impl.psi.formatter.FormattingDocumentModelImpl;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.FormattingModel;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;

public class XmlFormattingModelBuilder implements FormattingModelBuilder {
  
  @Nonnull
  public FormattingModel createModel(final PsiElement element, final CodeStyleSettings settings) {
    final ASTNode root = TreeUtil.getFileElement((TreeElement)SourceTreeToPsiMap.psiElementToTree(element));
    final FormattingDocumentModelImpl documentModel = FormattingDocumentModelImpl.createOn(element.getContainingFile());
    return new XmlFormattingModel(element.getContainingFile(),
                                                           new XmlBlock(root, null, null, new XmlPolicy(settings, documentModel), null, null, false),
                                                           documentModel);
  }

  public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
    return null;
  }
}
