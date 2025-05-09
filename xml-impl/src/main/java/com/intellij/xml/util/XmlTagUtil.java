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
package com.intellij.xml.util;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTagValue;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author peter
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class XmlTagUtil {
    private static final Map<String, Character> ourCharacterEntities;

    static {
        ourCharacterEntities = new HashMap<>();
        ourCharacterEntities.put("lt", '<');
        ourCharacterEntities.put("gt", '>');
        ourCharacterEntities.put("apos", '\'');
        ourCharacterEntities.put("quot", '\"');
        ourCharacterEntities.put("nbsp", '\u00a0');
        ourCharacterEntities.put("amp", '&');
    }

    /**
     * if text contains XML-sensitive characters (<,>), quote text with ![CDATA[ ... ]]
     *
     * @param text
     * @return quoted text
     */
    public static String getCDATAQuote(String text) {
        if (text == null) {
            return null;
        }
        String offensiveChars = "<>&\n";
        final int textLength = text.length();
        if (textLength > 0 && (Character.isWhitespace(text.charAt(0)) || Character.isWhitespace(text.charAt(textLength - 1)))) {
            return "<![CDATA[" + text + "]]>";
        }
        for (int i = 0; i < offensiveChars.length(); i++) {
            char c = offensiveChars.charAt(i);
            if (text.indexOf(c) != -1) {
                return "<![CDATA[" + text + "]]>";
            }
        }
        return text;
    }

    public static String getInlineQuote(String text) {
        if (text == null) {
            return null;
        }
        String offensiveChars = "<>&";
        for (int i = 0; i < offensiveChars.length(); i++) {
            char c = offensiveChars.charAt(i);
            if (text.indexOf(c) != -1) {
                return "<![CDATA[" + text + "]]>";
            }
        }
        return text;
    }


    public static String composeTagText(@NonNls String tagName, @NonNls String tagValue) {
        String result = "<" + tagName;
        if (tagValue == null || "".equals(tagValue)) {
            result += "/>";
        }
        else {
            result += ">" + getCDATAQuote(tagValue) + "</" + tagName + ">";
        }
        return result;
    }

    public static String[] getCharacterEntityNames() {
        Set<String> strings = ourCharacterEntities.keySet();
        return ArrayUtil.toStringArray(strings);
    }

    public static Character getCharacterByEntityName(String entityName) {
        return ourCharacterEntities.get(entityName);
    }

    @Nullable
    public static XmlToken getStartTagNameElement(@Nonnull XmlTag tag) {
        final ASTNode node = tag.getNode();
        if (node == null) {
            return null;
        }

        ASTNode current = node.getFirstChildNode();
        IElementType elementType;
        while (current != null
            && (elementType = current.getElementType()) != XmlTokenType.XML_NAME
            && elementType != XmlTokenType.XML_TAG_NAME) {
            current = current.getTreeNext();
        }
        return current == null ? null : (XmlToken)current.getPsi();
    }

    @Nullable
    public static XmlToken getEndTagNameElement(@Nonnull XmlTag tag) {
        final ASTNode node = tag.getNode();
        if (node == null) {
            return null;
        }

        ASTNode current = node.getLastChildNode();
        ASTNode prev = current;

        while (current != null) {
            final IElementType elementType = prev.getElementType();
            if ((elementType == XmlTokenType.XML_NAME || elementType == XmlTokenType.XML_TAG_NAME)
                && current.getElementType() == XmlTokenType.XML_END_TAG_START) {
                return (XmlToken)prev.getPsi();
            }

            prev = current;
            current = current.getTreePrev();
        }
        return null;
    }

    @Nonnull
    public static TextRange getTrimmedValueRange(final @Nonnull XmlTag tag) {
        XmlTagValue tagValue = tag.getValue();
        final String text = tagValue.getText();
        final String trimmed = text.trim();
        final int index = text.indexOf(trimmed);
        final int startOffset = tagValue.getTextRange().getStartOffset() - tag.getTextRange().getStartOffset() + index;
        return new TextRange(startOffset, startOffset + trimmed.length());
    }

    @Nullable
    public static TextRange getStartTagRange(@Nonnull XmlTag tag) {
        XmlToken tagName = getStartTagNameElement(tag);
        return getTag(tagName, XmlTokenType.XML_START_TAG_START);
    }


    @Nullable
    public static TextRange getEndTagRange(@Nonnull XmlTag tag) {
        XmlToken tagName = getEndTagNameElement(tag);

        return getTag(tagName, XmlTokenType.XML_END_TAG_START);
    }

    private static TextRange getTag(XmlToken tagName, IElementType tagStart) {
        if (tagName != null) {
            PsiElement s = tagName.getPrevSibling();

            while (s != null && s.getNode().getElementType() != tagStart) {
                s = s.getPrevSibling();
            }

            PsiElement f = tagName.getNextSibling();

            while (f != null
                && !(f.getNode().getElementType() == XmlTokenType.XML_TAG_END
                || f.getNode().getElementType() == XmlTokenType.XML_EMPTY_ELEMENT_END)) {
                f = f.getNextSibling();
            }
            if (s != null && f != null) {
                return new TextRange(s.getTextRange().getStartOffset(), f.getTextRange().getEndOffset());
            }
        }
        return null;
    }

    public static String escapeString(@Nullable final String str, final boolean escapeWhiteSpace) {
        return XmlStringUtil.escapeString(str, escapeWhiteSpace);
    }
}
