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
package consulo.xml.lang.html;

import javax.annotation.Nonnull;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.xml.lang.xml.XMLParserDefinition;
import consulo.xml.lexer.HtmlLexer;
import consulo.language.file.FileViewProvider;
import consulo.xml.psi.impl.source.html.HtmlFileImpl;
import consulo.language.ast.TokenSet;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTokenType;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiFile;

/**
 * @author max
 */
public class HTMLParserDefinition implements ParserDefinition {
  @Override
  @Nonnull
  public Lexer createLexer(@Nonnull LanguageVersion languageVersion) {
    return new HtmlLexer();
  }

  @Override
  @Nonnull
  public IFileElementType getFileNodeType() {
    return XmlElementType.HTML_FILE;
  }

  @Override
  @Nonnull
  public TokenSet getWhitespaceTokens(@Nonnull LanguageVersion languageVersion) {
    return XmlTokenType.WHITESPACES;
  }

  @Override
  @Nonnull
  public TokenSet getCommentTokens(LanguageVersion languageVersion) {
    return XmlTokenType.COMMENTS;
  }

  @Override
  @Nonnull
  public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
    return TokenSet.EMPTY;
  }

  @Override
  @Nonnull
  public PsiParser createParser(@Nonnull LanguageVersion languageVersion) {
    return new HTMLParser();
  }

  @Override
  public PsiFile createFile(FileViewProvider viewProvider) {
    return new HtmlFileImpl(viewProvider);
  }

  @Override
  public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    final Lexer lexer = createLexer(LanguageVersionUtil.findDefaultVersion(HTMLLanguage.INSTANCE));
    return XMLParserDefinition.canStickTokensTogetherByLexerInXml(left, right, lexer, 0);
  }
}
