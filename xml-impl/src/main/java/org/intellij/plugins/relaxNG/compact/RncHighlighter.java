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

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.HighlighterColors;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.lexer.Lexer;
import org.intellij.plugins.relaxNG.compact.lexer.CompactSyntaxLexerAdapter;
import consulo.colorScheme.TextAttributesKey;

/**
 * @author sweinreuter
 * @since 2007-08-04
 */
public class RncHighlighter extends SyntaxHighlighterBase {
  @Override
  @Nonnull
  public Lexer getHighlightingLexer() {
    return new CompactSyntaxLexerAdapter();
  }

  private static final Map<IElementType, TextAttributesKey> ourMap1;

  static {
    ourMap1 = new HashMap<>();

    fillMap(ourMap1, RncTokenTypes.KEYWORDS, DefaultLanguageHighlighterColors.KEYWORD);
    fillMap(ourMap1, RncTokenTypes.OPERATORS, DefaultLanguageHighlighterColors.OPERATION_SIGN);

    fillMap(ourMap1, RncTokenTypes.STRINGS, DefaultLanguageHighlighterColors.STRING);

    ourMap1.put(RncTokenTypes.LBRACE, DefaultLanguageHighlighterColors.BRACES);
    ourMap1.put(RncTokenTypes.RBRACE, DefaultLanguageHighlighterColors.BRACES);

    ourMap1.put(RncTokenTypes.LBRACKET, DefaultLanguageHighlighterColors.BRACKETS);
    ourMap1.put(RncTokenTypes.RBRACKET, DefaultLanguageHighlighterColors.BRACKETS);

    ourMap1.put(RncTokenTypes.LPAREN, DefaultLanguageHighlighterColors.PARENTHESES);
    ourMap1.put(RncTokenTypes.RPAREN, DefaultLanguageHighlighterColors.PARENTHESES);

    ourMap1.put(RncTokenTypes.COMMA, DefaultLanguageHighlighterColors.COMMA);

    fillMap(ourMap1, RncTokenTypes.DOC_TOKENS, DefaultLanguageHighlighterColors.DOC_COMMENT);
    fillMap(ourMap1, RncTokenTypes.COMMENTS, DefaultLanguageHighlighterColors.LINE_COMMENT);

    fillMap(ourMap1, RncTokenTypes.IDENTIFIERS, DefaultLanguageHighlighterColors.LOCAL_VARIABLE);

    ourMap1.put(RncTokenTypes.ILLEGAL_CHAR, HighlighterColors.BAD_CHARACTER);
  }

  @Override
  @Nonnull
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return pack(ourMap1.get(tokenType));
  }
}