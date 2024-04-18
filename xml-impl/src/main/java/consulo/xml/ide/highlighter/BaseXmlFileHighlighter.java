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
import consulo.util.collection.MultiMap;
import consulo.xml.editor.XmlHighlighterColors;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

public abstract class BaseXmlFileHighlighter extends SyntaxHighlighterBase {
  protected static void storeDefaults(MultiMap<IElementType, TextAttributesKey> keys) {
    keys.putValue(XmlTokenType.XML_DATA_CHARACTERS, XmlHighlighterColors.XML_TAG_DATA);

    keys.putValue(XmlTokenType.XML_COMMENT_START, XmlHighlighterColors.XML_COMMENT);
    keys.putValue(XmlTokenType.XML_COMMENT_END, XmlHighlighterColors.XML_COMMENT);
    keys.putValue(XmlTokenType.XML_COMMENT_CHARACTERS, XmlHighlighterColors.XML_COMMENT);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_COMMENT_END, XmlHighlighterColors.XML_COMMENT);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_COMMENT_END_START, XmlHighlighterColors.XML_COMMENT);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_COMMENT_START, XmlHighlighterColors.XML_COMMENT);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_COMMENT_START_END, XmlHighlighterColors.XML_COMMENT);

    keys.putValue(XmlTokenType.XML_START_TAG_START, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_END_TAG_START, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_TAG_END, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_EMPTY_ELEMENT_END, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_TAG_NAME, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.TAG_WHITE_SPACE, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_NAME, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_IGNORE, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_INCLUDE, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_EQ, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_TAG_CHARACTERS, XmlHighlighterColors.XML_TAG);

    keys.putValue(XmlTokenType.XML_TAG_NAME, XmlHighlighterColors.XML_TAG_NAME);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_INCLUDE, XmlHighlighterColors.XML_TAG_NAME);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_INCLUDE, XmlHighlighterColors.XML_TAG_NAME);
    keys.putValue(XmlTokenType.XML_NAME, XmlHighlighterColors.XML_ATTRIBUTE_NAME);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN, XmlHighlighterColors.XML_ATTRIBUTE_VALUE);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER, XmlHighlighterColors.XML_ATTRIBUTE_VALUE);
    keys.putValue(XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER, XmlHighlighterColors.XML_ATTRIBUTE_VALUE);
    keys.putValue(XmlTokenType.XML_EQ, XmlHighlighterColors.XML_ATTRIBUTE_NAME);
    keys.putValue(XmlTokenType.XML_TAG_CHARACTERS, XmlHighlighterColors.XML_ATTRIBUTE_VALUE);

    keys.putValue(XmlTokenType.XML_BAD_CHARACTER, HighlighterColors.BAD_CHARACTER);

    keys.putValue(XmlTokenType.XML_DECL_START, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_DECL_START, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_CONDITIONAL_SECTION_START, XmlHighlighterColors.XML_PROLOGUE);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_SECTION_START, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_CONDITIONAL_SECTION_END, XmlHighlighterColors.XML_PROLOGUE);
    keys.putValue(XmlTokenType.XML_CONDITIONAL_SECTION_END, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_DECL_END, XmlHighlighterColors.XML_PROLOGUE);
    keys.putValue(XmlTokenType.XML_DECL_END, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_PI_START, XmlHighlighterColors.XML_PROLOGUE);
    keys.putValue(XmlTokenType.XML_PI_END, XmlHighlighterColors.XML_PROLOGUE);
    keys.putValue(XmlTokenType.XML_DOCTYPE_END, XmlHighlighterColors.XML_PROLOGUE);
    keys.putValue(XmlTokenType.XML_DOCTYPE_END, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_DOCTYPE_START, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_DOCTYPE_START, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_DOCTYPE_SYSTEM, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_DOCTYPE_SYSTEM, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_DOCTYPE_PUBLIC, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_ATTLIST_DECL_START, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_ATTLIST_DECL_START, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_ELEMENT_DECL_START, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_ELEMENT_DECL_START, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_ENTITY_DECL_START, XmlHighlighterColors.XML_TAG);
    keys.putValue(XmlTokenType.XML_ENTITY_DECL_START, XmlHighlighterColors.XML_TAG_NAME);

    keys.putValue(XmlTokenType.XML_CHAR_ENTITY_REF, XmlHighlighterColors.XML_ENTITY_REFERENCE);
    keys.putValue(XmlTokenType.XML_ENTITY_REF_TOKEN, XmlHighlighterColors.XML_ENTITY_REFERENCE);
  }

  private final Application myApplication;

  public BaseXmlFileHighlighter(Application application) {
    myApplication = application;
  }

  protected abstract ExtensionPointCacheKey<EmbeddedTokenHighlighter, MultiMap<IElementType, TextAttributesKey>> getCacheKey();

  @Override
  @Nonnull
  public TextAttributesKey[] getTokenHighlights(@Nonnull IElementType tokenType) {
    MultiMap<IElementType, TextAttributesKey> map =
      myApplication.getExtensionPoint(EmbeddedTokenHighlighter.class).getOrBuildCache(getCacheKey());

    return SyntaxHighlighterBase.pack(XmlHighlighterColors.HTML_CODE, map.get(tokenType).toArray(new TextAttributesKey[0]));
  }
}
