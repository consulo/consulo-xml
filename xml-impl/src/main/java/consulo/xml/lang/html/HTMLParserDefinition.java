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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.xml.lang.xml.XMLParserDefinition;
import consulo.xml.lexer.HtmlLexer;
import consulo.xml.psi.impl.source.html.HtmlFileImpl;
import consulo.xml.language.psi.XmlElementType;
import consulo.xml.language.psi.XmlTokenType;
import consulo.xml.psi.XmlElementTokenTypeImpl;


/**
 * @author max
 */
@ExtensionImpl
public class HTMLParserDefinition implements ParserDefinition
{
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}

	@Override
	public Lexer createLexer(LanguageVersion languageVersion)
	{
		return new HtmlLexer();
	}

	@Override
	public IFileElementType getFileNodeType()
	{
		return XmlElementTokenTypeImpl.HTML_FILE;
	}

	@Override
	public TokenSet getWhitespaceTokens(LanguageVersion languageVersion)
	{
		return XmlTokenType.WHITESPACES;
	}

	@Override
	public TokenSet getCommentTokens(LanguageVersion languageVersion)
	{
		return XmlTokenType.COMMENTS;
	}

	@Override
	public TokenSet getStringLiteralElements(LanguageVersion languageVersion)
	{
		return TokenSet.EMPTY;
	}

	@Override
	public PsiParser createParser(LanguageVersion languageVersion)
	{
		return new HTMLParser();
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider)
	{
		return new HtmlFileImpl(viewProvider);
	}

	@Override
	public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right)
	{
		final Lexer lexer = createLexer(LanguageVersionUtil.findDefaultVersion(HTMLLanguage.INSTANCE));
		return XMLParserDefinition.canStickTokensTogetherByLexerInXml(left, right, lexer, 0);
	}
}
