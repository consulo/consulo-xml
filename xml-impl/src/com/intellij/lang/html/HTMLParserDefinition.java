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
package com.intellij.lang.html;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lang.xml.XMLParserDefinition;
import com.intellij.lexer.HtmlLexer;
import com.intellij.lexer.Lexer;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTokenType;
import consulo.lang.LanguageVersion;
import consulo.lang.util.LanguageVersionUtil;

/**
 * @author max
 */
public class HTMLParserDefinition implements ParserDefinition {
  @Override
  @NotNull
  public Lexer createLexer(@NotNull LanguageVersion languageVersion) {
    return new HtmlLexer();
  }

  @Override
  @NotNull
  public IFileElementType getFileNodeType() {
    return XmlElementType.HTML_FILE;
  }

  @Override
  @NotNull
  public TokenSet getWhitespaceTokens(@NotNull LanguageVersion languageVersion) {
    return XmlTokenType.WHITESPACES;
  }

  @Override
  @NotNull
  public TokenSet getCommentTokens(LanguageVersion languageVersion) {
    return XmlTokenType.COMMENTS;
  }

  @Override
  @NotNull
  public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
    return TokenSet.EMPTY;
  }

  @Override
  @NotNull
  public PsiParser createParser(@NotNull LanguageVersion languageVersion) {
    return new HTMLParser();
  }

  @Override
  @NotNull
  public PsiElement createElement(ASTNode node) {
    return PsiUtilCore.NULL_PSI_ELEMENT;
  }

  @Override
  public PsiFile createFile(FileViewProvider viewProvider) {
    return new HtmlFileImpl(viewProvider);
  }

  @Override
  public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
    final Lexer lexer = createLexer(LanguageVersionUtil.findDefaultVersion(HTMLLanguage.INSTANCE));
    return XMLParserDefinition.canStickTokensTogetherByLexerInXml(left, right, lexer, 0);
  }
}
