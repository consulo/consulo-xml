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
package consulo.xml.lang.xhtml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.xml.lang.xml.XMLParserDefinition;
import consulo.xml.lexer.XHtmlLexer;
import consulo.xml.psi.impl.source.xml.XmlFileImpl;
import consulo.xml.psi.xml.XmlElementType;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
@ExtensionImpl
public class XHTMLParserDefinition extends XMLParserDefinition
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}

	@Override
	@Nonnull
	public Lexer createLexer(@Nonnull LanguageVersion languageVersion)
	{
		return new XHtmlLexer();
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right)
	{
		final Lexer lexer = createLexer(LanguageVersionUtil.findDefaultVersion(XHTMLLanguage.INSTANCE));
		return canStickTokensTogetherByLexerInXml(left, right, lexer, 0);
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider)
	{
		return new XmlFileImpl(viewProvider, XmlElementType.XHTML_FILE);
	}

}
