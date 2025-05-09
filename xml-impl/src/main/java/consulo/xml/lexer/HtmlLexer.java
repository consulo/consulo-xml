/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.xml.lexer;

import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.FlexAdapter;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.MergingLexerAdapter;
import consulo.language.util.LanguageUtil;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;

/**
 * @author Maxim.Mossienko
 */
public class HtmlLexer extends BaseHtmlLexer {
  public static final String INLINE_STYLE_NAME = "css-ruleset-block";

  private IElementType myTokenType;
  private int myTokenStart;
  private int myTokenEnd;

  private IElementType myInlineStyleElementType;
  private IElementType myInlineScriptElementType;

  public HtmlLexer() {
    this(new MergingLexerAdapter(new FlexAdapter(new _HtmlLexer()), TOKENS_TO_MERGE), true);
  }

  protected HtmlLexer(Lexer _baseLexer, boolean _caseInsensitive) {
    super(_baseLexer, _caseInsensitive);

    ExternalPluginElementTypeHolder elementTypeHolder = ExternalPluginElementTypeHolder.getInstance();
    myInlineStyleElementType = elementTypeHolder.getInlineStyleElementType();
    myInlineScriptElementType = elementTypeHolder.getInlineScriptElementType();
  }

  @Override
  public void start(@Nonnull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    myTokenType = null;
    super.start(buffer, startOffset, endOffset, initialState);
  }

  @Override
  public void advance() {
    myTokenType = null;
    super.advance();
  }

  @Override
  public IElementType getTokenType() {
    if (myTokenType != null) {
      return myTokenType;
    }
    IElementType tokenType = super.getTokenType();

    myTokenStart = super.getTokenStart();
    myTokenEnd = super.getTokenEnd();

    if (hasSeenStyle()) {
      if (hasSeenTag() && isStartOfEmbeddmentTagContent(tokenType)) {
        myTokenEnd = skipToTheEndOfTheEmbeddment();
        IElementType currentStylesheetElementType = getCurrentStylesheetElementType();
        tokenType = currentStylesheetElementType == null ? XmlTokenType.XML_DATA_CHARACTERS : currentStylesheetElementType;
      } else if (myInlineStyleElementType != null && isStartOfEmbeddmentAttributeValue(tokenType) && hasSeenAttribute()) {
        tokenType = myInlineStyleElementType;
      }
    } else if (hasSeenScript()) {
      if (hasSeenTag() && isStartOfEmbeddmentTagContent(tokenType)) {
        Language scriptLanguage = getScriptLanguage();
        if (scriptLanguage == null || LanguageUtil.isInjectableLanguage(scriptLanguage)) {
          myTokenEnd = skipToTheEndOfTheEmbeddment();
          IElementType currentScriptElementType = getCurrentScriptElementType();
          tokenType = currentScriptElementType == null ? XmlTokenType.XML_DATA_CHARACTERS : currentScriptElementType;
        }
      } else if (hasSeenAttribute() && isStartOfEmbeddmentAttributeValue(tokenType) && myInlineScriptElementType != null) {
        myTokenEnd = skipToTheEndOfTheEmbeddment();
        tokenType = myInlineScriptElementType;
      }
    }

    return myTokenType = tokenType;
  }

  private static boolean isStartOfEmbeddmentAttributeValue(final IElementType tokenType) {
    return tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN;
  }

  private static boolean isStartOfEmbeddmentTagContent(final IElementType tokenType) {
    return (tokenType == XmlTokenType.XML_DATA_CHARACTERS || tokenType == XmlTokenType.XML_CDATA_START || tokenType == XmlTokenType.XML_COMMENT_START || tokenType == XmlTokenType
        .XML_REAL_WHITE_SPACE || tokenType == TokenType.WHITE_SPACE);
  }

  @Override
  protected boolean isHtmlTagState(int state) {
    return state == _HtmlLexer.START_TAG_NAME || state == _HtmlLexer.END_TAG_NAME;
  }

  @Override
  public int getTokenStart() {
    if (myTokenType != null) {
      return myTokenStart;
    }
    return super.getTokenStart();
  }

  @Override
  public int getTokenEnd() {
    if (myTokenType != null) {
      return myTokenEnd;
    }
    return super.getTokenEnd();
  }
}
