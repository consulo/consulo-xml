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
package consulo.xml.lang.xml;

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
import consulo.xml.lexer.XmlLexer;
import consulo.xml.psi.impl.source.parsing.xml.XmlParser;
import consulo.xml.psi.impl.source.xml.XmlFileImpl;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

/**
 * @author max
 */
@ExtensionImpl
public class XMLParserDefinition implements ParserDefinition
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}

	@Override
	@Nonnull
	public Lexer createLexer(@Nonnull LanguageVersion languageVersion)
	{
		return new XmlLexer();
	}

	@Override
	@Nonnull
	public IFileElementType getFileNodeType()
	{
		return XmlElementType.XML_FILE;
	}

	@Override
	@Nonnull
	public TokenSet getWhitespaceTokens(@Nonnull LanguageVersion languageVersion)
	{
		return XmlTokenType.WHITESPACES;
	}

	@Override
	@Nonnull
	public TokenSet getCommentTokens(LanguageVersion languageVersion)
	{
		return XmlTokenType.COMMENTS;
	}

	@Override
	@Nonnull
	public TokenSet getStringLiteralElements(LanguageVersion languageVersion)
	{
		return TokenSet.EMPTY;
	}

	@Override
	@Nonnull
	public PsiParser createParser(@Nonnull LanguageVersion languageVersion)
	{
		return new XmlParser();
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider)
	{
		return new XmlFileImpl(viewProvider, XmlElementType.XML_FILE);
	}

	@Override
	public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right)
	{
		final Lexer lexer = createLexer(LanguageVersionUtil.findDefaultVersion(XMLLanguage.INSTANCE));
		return canStickTokensTogetherByLexerInXml(left, right, lexer, 0);
	}

	public static SpaceRequirements canStickTokensTogetherByLexerInXml(final ASTNode left,
																	   final ASTNode right,
																	   final Lexer lexer,
																	   int state)
	{
		if(left.getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN ||
				right.getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
		{
			return SpaceRequirements.MUST_NOT;
		}
		if(left.getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER && right.getElementType() == XmlTokenType.XML_NAME)
		{
			return SpaceRequirements.MUST;
		}
		if(left.getElementType() == XmlTokenType.XML_NAME && right.getElementType() == XmlTokenType.XML_NAME)
		{
			return SpaceRequirements.MUST;
		}
		return SpaceRequirements.MAY;
	}
}
