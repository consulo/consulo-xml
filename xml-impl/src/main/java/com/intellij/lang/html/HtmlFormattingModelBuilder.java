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

/*
 * @author max
 */
package com.intellij.lang.html;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ide.impl.psi.formatter.FormattingDocumentModelImpl;
import com.intellij.psi.formatter.xml.HtmlPolicy;
import com.intellij.psi.formatter.xml.XmlBlock;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import com.intellij.lang.xml.XmlFormattingModel;
import consulo.language.codeStyle.FormattingModel;

import javax.annotation.Nonnull;

public class HtmlFormattingModelBuilder implements FormattingModelBuilder {
  @Nonnull
    public FormattingModel createModel(final PsiElement element, final CodeStyleSettings settings) {
    final PsiFile psiFile = element.getContainingFile();
    final FormattingDocumentModelImpl documentModel = FormattingDocumentModelImpl.createOn(psiFile);
    return new XmlFormattingModel(psiFile,
                                                           new XmlBlock(SourceTreeToPsiMap.psiElementToTree(psiFile),
                                                                        null, null, new HtmlPolicy(settings, documentModel), null, null, false),
                                                           documentModel);
  }

  public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
    return null;
  }
}