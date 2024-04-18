/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.xml.ide.highlighter;

import com.intellij.xml.highlighter.EmbeddedTokenHighlighter;
import consulo.application.Application;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.lexer.Lexer;
import consulo.util.collection.MultiMap;
import consulo.xml.editor.XmlHighlighterColors;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lexer.HtmlHighlightingLexer;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

public class HtmlFileHighlighter extends SyntaxHighlighterBase {
  private static final ExtensionPointCacheKey<EmbeddedTokenHighlighter, MultiMap<IElementType, TextAttributesKey>> CACHE_KEY =
    ExtensionPointCacheKey.create("HtmlFileHighlighter.tokens", embeddedTokenHighlighterExtensionWalker -> {
      MultiMap<IElementType, TextAttributesKey> map = MultiMap.createLinked();

      storeDefaults(map);

      embeddedTokenHighlighterExtensionWalker.walk(it -> map.putAllValues(it.getEmbeddedTokenAttributes(HTMLLanguage.INSTANCE)));

      return map;
    });

  private static void storeDefaults(MultiMap<IElementType, TextAttributesKey> keys) {
    keys.putValue(XmlTokenType.XML_COMMENT_START, XmlHighlighterColors.HTML_COMMENT);
    keys.putValue(XmlTokenType.XML_COMMENT_END, XmlHighlighterColors.HTML_COMMENT);
    keys.putValue(XmlTokenType.XML_COMMENT_CHARACTERS, XmlHighlighterColors.HTML_COMMENT);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_COMMENT_END, XmlHighlighterColors.HTML_COMMENT);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_COMMENT_END_START, XmlHighlighterColors.HTML_COMMENT);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_COMMENT_START, XmlHighlighterColors.HTML_COMMENT);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_COMMENT_START_END, XmlHighlighterColors.HTML_COMMENT);

    keys.putValue(XmlTokenType.XML_START_TAG_START, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_END_TAG_START, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_TAG_END, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_EMPTY_ELEMENT_END, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_TAG_NAME, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.TAG_WHITE_SPACE, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_NAME, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_TAG_CHARACTERS, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_EQ, XmlHighlighterColors.HTML_TAG);

    keys.putValue(XmlTokenType.XML_TAG_NAME, XmlHighlighterColors.HTML_TAG_NAME);
    keys.putValue(XmlTokenType.XML_NAME, XmlHighlighterColors.HTML_ATTRIBUTE_NAME);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, XmlHighlighterColors.HTML_ATTRIBUTE_VALUE);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER, XmlHighlighterColors.HTML_ATTRIBUTE_VALUE);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER, XmlHighlighterColors.HTML_ATTRIBUTE_VALUE);
    keys.putValue(XmlTokenType.XML_EQ, XmlHighlighterColors.HTML_ATTRIBUTE_NAME);

    keys.putValue(XmlTokenType.XML_PI_START, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_PI_END, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_PI_TARGET, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_PI_TARGET, XmlHighlighterColors.HTML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_DOCTYPE_START, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_DOCTYPE_END, XmlHighlighterColors.HTML_TAG);
    keys.putValue(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.HTML_TAG);

    keys.putValue(XmlTokenType.XML_CHAR_ENTITY_REF, XmlHighlighterColors.HTML_ENTITY_REFERENCE);
    keys.putValue(XmlTokenType.XML_ENTITY_REF_TOKEN, XmlHighlighterColors.HTML_ENTITY_REFERENCE);

    keys.putValue(XmlTokenType.XML_BAD_CHARACTER, HighlighterColors.BAD_CHARACTER);
  }

  private final Application myApplication;

  public HtmlFileHighlighter(Application application) {
    myApplication = application;
  }

  @Override
  @Nonnull
  public Lexer getHighlightingLexer() {
    return new HtmlHighlightingLexer();
  }

  @Override
  @Nonnull
  public TextAttributesKey[] getTokenHighlights(@Nonnull IElementType tokenType) {
    MultiMap<IElementType, TextAttributesKey> map =
      myApplication.getExtensionPoint(EmbeddedTokenHighlighter.class).getOrBuildCache(CACHE_KEY);

    return SyntaxHighlighterBase.pack(XmlHighlighterColors.HTML_CODE, map.get(tokenType).toArray(new TextAttributesKey[0]));
  }
}
