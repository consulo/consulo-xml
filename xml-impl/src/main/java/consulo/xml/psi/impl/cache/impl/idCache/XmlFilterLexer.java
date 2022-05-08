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
package consulo.xml.psi.impl.cache.impl.idCache;

import consulo.ide.impl.psi.impl.cache.impl.BaseFilterLexer;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.Lexer;
import consulo.ide.impl.psi.impl.cache.impl.OccurrenceConsumer;
import consulo.language.psi.search.UsageSearchContext;
import consulo.language.ast.TokenSet;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTokenType;

public class XmlFilterLexer extends BaseFilterLexer {
  static final TokenSet ourNoWordsTokenSet = TokenSet.create(
    XmlTokenType.TAG_WHITE_SPACE,
    TokenType.WHITE_SPACE,
    XmlTokenType.XML_REAL_WHITE_SPACE,
    XmlTokenType.XML_EQ,
    XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER,
    XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER,
    XmlTokenType.XML_START_TAG_START,
    XmlTokenType.XML_EMPTY_ELEMENT_END,
    XmlTokenType.XML_END_TAG_START,
    XmlTokenType.XML_TAG_END,
    XmlTokenType.XML_DOCTYPE_END,
    XmlTokenType.XML_COMMENT_START,
    XmlTokenType.XML_COMMENT_END,
    XmlTokenType.XML_PI_START,
    XmlTokenType.XML_PI_END,
    XmlTokenType.XML_CDATA_END
  );

  public XmlFilterLexer(Lexer originalLexer, OccurrenceConsumer table) {
    super(originalLexer, table);
  }

  public void advance() {
    final IElementType tokenType = myDelegate.getTokenType();

    if (tokenType == XmlElementType.XML_COMMENT_CHARACTERS) {
      scanWordsInToken(UsageSearchContext.IN_COMMENTS, false, false);
      advanceTodoItemCountsInToken();
    }

    if (tokenType == XmlElementType.XML_ATTRIBUTE_VALUE_TOKEN) {
      scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT | UsageSearchContext.IN_FOREIGN_LANGUAGES, true, false);
    }
    else if (tokenType == XmlElementType.XML_NAME || tokenType == XmlElementType.XML_DATA_CHARACTERS) {
      scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT | UsageSearchContext.IN_FOREIGN_LANGUAGES, false, false);
    }
    else if (tokenType == XmlElementType.XML_ENTITY_REF_TOKEN || tokenType == XmlElementType.XML_CHAR_ENTITY_REF) {
      scanWordsInToken(UsageSearchContext.IN_CODE, false, false);
    }
    else if (tokenType == XmlElementType.XML_TEXT) {
      scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT | UsageSearchContext.IN_FOREIGN_LANGUAGES, false, false);
    }
    else if (tokenType == XmlTokenType.XML_TAG_CHARACTERS) {
      scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT | UsageSearchContext.IN_FOREIGN_LANGUAGES, false, false);
    }
    else if (!ourNoWordsTokenSet.contains(tokenType)) {
      scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT, false, false);
    }

    myDelegate.advance();
  }
}
