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

package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.language.Language;
import consulo.language.ast.*;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;
import org.intellij.plugins.relaxNG.compact.lexer.CompactSyntaxLexerAdapter;
import org.intellij.plugins.relaxNG.compact.parser.RncParser;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.impl.RncElementImpl;
import org.intellij.plugins.relaxNG.compact.psi.impl.RncFileImpl;

import jakarta.annotation.Nonnull;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 10.08.2007
 */
@ExtensionImpl
public class RncParserDefinition implements ParserDefinition
{
	public static final IFileElementType FILE_ELEMENT_TYPE = new IFileElementType(RngCompactLanguage.INSTANCE);
	private static final TokenSet myCommentTypes = TokenSet.orSet(RncTokenTypes.COMMENTS, RncTokenTypes.DOC_TOKENS);

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}

	@Override
	@Nonnull
	public Lexer createLexer(LanguageVersion languageVersion)
	{
		return new CompactSyntaxLexerAdapter();
	}

	@Override
	public PsiParser createParser(LanguageVersion languageVersion)
	{
		return new RncParser();
	}

	@Override
	public IFileElementType getFileNodeType()
	{
		return FILE_ELEMENT_TYPE;
	}

	@Override
	@Nonnull
	public TokenSet getWhitespaceTokens(LanguageVersion languageVersion)
	{
		return TokenSet.create(TokenType.WHITE_SPACE);
	}

	@Override
	@Nonnull
	public TokenSet getCommentTokens(LanguageVersion languageVersion)
	{
		return myCommentTypes;
	}

	@Override
	@Nonnull
	public TokenSet getStringLiteralElements(LanguageVersion languageVersion)
	{
		return TokenSet.create(RncTokenTypes.LITERAL);
	}

	@Override
	@Nonnull
	@SuppressWarnings({"unchecked"})
	public PsiElement createElement(ASTNode node)
	{
		return new MyRncElement(node);
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider)
	{
		return new RncFileImpl(viewProvider);
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right)
	{
		return SpaceRequirements.MAY;
	}

	private static class MyRncElement extends RncElementImpl
	{
		public MyRncElement(ASTNode node)
		{
			super(node);
		}

		@Override
		public void accept(@Nonnull RncElementVisitor visitor)
		{
			visitor.visitElement(this);
		}
	}
}
