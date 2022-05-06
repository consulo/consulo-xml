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

import javax.annotation.Nonnull;

import consulo.language.codeStyle.FormattingModel;
import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.codeStyle.FormattingModelProvider;
import consulo.language.psi.PsiFile;

public class RncFormattingModelBuilder implements FormattingModelBuilder {
  @Override
  @Nonnull
  public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
    return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), new RncBlock(element.getNode()), settings);
  }

  @Override
  public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
    // TODO
    return null;
  }
}