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

import javax.annotation.Nonnull;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.ast.TokenSet;
import consulo.language.version.LanguageVersion;

public class HTMLParser implements PsiParser {

  @Nonnull
  public ASTNode parse(@Nonnull final IElementType root, @Nonnull final PsiBuilder builder, @Nonnull LanguageVersion languageVersion) {
    builder.enforceCommentTokens(TokenSet.EMPTY);
    final PsiBuilder.Marker file = builder.mark();
    new HtmlParsing(builder).parseDocument();
    file.done(root);
    return builder.getTreeBuilt();
  }

}