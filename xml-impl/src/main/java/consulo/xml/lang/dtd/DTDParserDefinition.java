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
package consulo.xml.lang.dtd;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiFile;
import consulo.language.util.LanguageUtil;
import consulo.language.version.LanguageVersion;
import consulo.xml.lang.xml.XMLParserDefinition;
import consulo.xml.lexer.DtdLexer;
import consulo.xml.psi.impl.source.parsing.xml.DtdParsing;
import consulo.xml.psi.impl.source.xml.XmlFileImpl;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlEntityDecl;

import javax.annotation.Nonnull;

/**
 * @author max
 */
public class DTDParserDefinition extends XMLParserDefinition {
  public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return LanguageUtil.canStickTokensTogetherByLexer(left, right, new DtdLexer(false));
  }

  public PsiFile createFile(FileViewProvider viewProvider) {
    return new XmlFileImpl(viewProvider, XmlElementType.DTD_FILE);
  }

  @Nonnull
  @Override
  public PsiParser createParser(@Nonnull LanguageVersion languageVersion) {
    return new PsiParser() {
      @Nonnull
      @Override
      public ASTNode parse(@Nonnull IElementType root, @Nonnull PsiBuilder builder, @Nonnull LanguageVersion languageVersion) {
        return new DtdParsing(root, XmlEntityDecl.EntityContextType.GENERIC_XML, builder).parse();
      }
    };
  }

  @Nonnull
  @Override
  public IFileElementType getFileNodeType() {
    return XmlElementType.DTD_FILE;
  }

  @Nonnull
  @Override
  public Lexer createLexer(@Nonnull LanguageVersion languageVersion) {
    return new DtdLexer(false);
  }
}
