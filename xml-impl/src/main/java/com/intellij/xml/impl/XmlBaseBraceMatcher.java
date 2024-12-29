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
package com.intellij.xml.impl;

import com.intellij.xml.util.HtmlUtil;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.editor.action.BraceMatchingUtil;
import consulo.language.editor.highlight.VirtualFileBraceMatcher;
import consulo.language.editor.highlight.XmlAwareBraceMatcher;
import consulo.language.psi.PsiFile;
import consulo.util.collection.BidirectionalMap;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.ide.highlighter.XHtmlFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.ide.highlighter.XmlLikeFileType;
import consulo.xml.psi.tree.xml.IXmlLeafElementType;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author Maxim.Mossienko
 * Date: Apr 15, 2008
 * Time: 4:27:25 PM
 */
public abstract class XmlBaseBraceMatcher implements XmlAwareBraceMatcher, VirtualFileBraceMatcher {
    private static final int XML_TAG_TOKEN_GROUP = 1;
    private static final int XML_VALUE_DELIMITER_GROUP = 2;

    private static final BidirectionalMap<IElementType, IElementType> PAIRING_TOKENS = new BidirectionalMap<>();

    static {
        PAIRING_TOKENS.put(XmlTokenType.XML_TAG_END, XmlTokenType.XML_START_TAG_START);
        PAIRING_TOKENS.put(XmlTokenType.XML_CDATA_START, XmlTokenType.XML_CDATA_END);
        PAIRING_TOKENS.put(XmlTokenType.XML_EMPTY_ELEMENT_END, XmlTokenType.XML_START_TAG_START);
        PAIRING_TOKENS.put(XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER, XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER);
    }

    public int getBraceTokenGroupId(IElementType tokenType) {
        final Language l = tokenType.getLanguage();
        PairedBraceMatcher matcher = PairedBraceMatcher.forLanguage(l);

        if (matcher != null) {
            BracePair[] pairs = matcher.getPairs();
            for (BracePair pair : pairs) {
                if (pair.getLeftBraceType() == tokenType || pair.getRightBraceType() == tokenType) {
                    return l.hashCode();
                }
            }
        }
        if (tokenType instanceof IXmlLeafElementType) {
            return tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER
                || tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER
                ? XML_VALUE_DELIMITER_GROUP
                : XML_TAG_TOKEN_GROUP;
        }
        else {
            return BraceMatchingUtil.UNDEFINED_TOKEN_GROUP;
        }
    }

    public boolean isLBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
        final IElementType tokenType = (IElementType)iterator.getTokenType();
        PairedBraceMatcher matcher = PairedBraceMatcher.forLanguage(tokenType.getLanguage());
        if (matcher != null) {
            BracePair[] pairs = matcher.getPairs();
            for (BracePair pair : pairs) {
                if (pair.getLeftBraceType() == tokenType) {
                    return true;
                }
            }
        }
        return tokenType == XmlTokenType.XML_START_TAG_START ||
            tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER ||
            tokenType == XmlTokenType.XML_CDATA_START;
    }

    public boolean isRBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
        final IElementType tokenType = (IElementType)iterator.getTokenType();
        PairedBraceMatcher matcher = PairedBraceMatcher.forLanguage(tokenType.getLanguage());
        if (matcher != null) {
            BracePair[] pairs = matcher.getPairs();
            for (BracePair pair : pairs) {
                if (pair.getRightBraceType() == tokenType) {
                    return true;
                }
            }
        }

        if (tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END ||
            tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER ||
            tokenType == XmlTokenType.XML_CDATA_END) {
            return true;
        }
        else if (tokenType == XmlTokenType.XML_TAG_END) {
            final boolean result = findEndTagStart(iterator);

            if (isFileTypeWithSingleHtmlTags(fileType)) {
                final String tagName = getTagName(fileText, iterator);

                if (tagName != null && HtmlUtil.isSingleHtmlTag(tagName)) {
                    return !result;
                }
            }

            return result;
        }
        else {
            return false;
        }
    }

    protected boolean isFileTypeWithSingleHtmlTags(final FileType fileType) {
        return fileType == HtmlFileType.INSTANCE;
    }

    public boolean isPairBraces(IElementType tokenType1, IElementType tokenType2) {
        PairedBraceMatcher matcher = PairedBraceMatcher.forLanguage(tokenType1.getLanguage());
        if (matcher != null) {
            BracePair[] pairs = matcher.getPairs();
            for (BracePair pair : pairs) {
                if (pair.getLeftBraceType() == tokenType1) {
                    return pair.getRightBraceType() == tokenType2;
                }
                if (pair.getRightBraceType() == tokenType1) {
                    return pair.getLeftBraceType() == tokenType2;
                }
            }
        }
        if (tokenType2.equals(PAIRING_TOKENS.get(tokenType1))) {
            return true;
        }
        List<IElementType> keys = PAIRING_TOKENS.getKeysByValue(tokenType1);
        return keys != null && keys.contains(tokenType2);
    }

    public boolean isStructuralBrace(HighlighterIterator iterator, CharSequence text, FileType fileType) {
        IElementType tokenType = (IElementType)iterator.getTokenType();

        PairedBraceMatcher matcher = PairedBraceMatcher.forLanguage(tokenType.getLanguage());
        if (matcher != null) {
            BracePair[] pairs = matcher.getPairs();
            for (BracePair pair : pairs) {
                if ((pair.getLeftBraceType() == tokenType || pair.getRightBraceType() == tokenType) &&
                    pair.isStructural()) {
                    return true;
                }
            }
        }
        if (fileType instanceof XmlLikeFileType) {
            return isXmlStructuralBrace(iterator, text, fileType, tokenType);
        }
        return false;
    }

    protected boolean isXmlStructuralBrace(HighlighterIterator iterator, CharSequence text, FileType fileType, IElementType tokenType) {
        return tokenType == XmlTokenType.XML_START_TAG_START ||
            tokenType == XmlTokenType.XML_TAG_END ||
            tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END ||
            tokenType == XmlTokenType.XML_TAG_END && isFileTypeWithSingleHtmlTags(fileType) && isEndOfSingleHtmlTag(text, iterator);
    }

    public boolean isPairedBracesAllowedBeforeType(@Nonnull final IElementType lbraceType, @Nullable final IElementType contextType) {
        return true;
    }

    public boolean isStrictTagMatching(final FileType fileType, final int braceGroupId) {
        switch (braceGroupId) {
            case XML_TAG_TOKEN_GROUP:
                // Other xml languages may have nonbalanced tag names
                return isStrictTagMatchingForFileType(fileType);

            default:
                return false;
        }
    }

    protected boolean isStrictTagMatchingForFileType(final FileType fileType) {
        return fileType == XmlFileType.INSTANCE || fileType == XHtmlFileType.INSTANCE;
    }

    public boolean areTagsCaseSensitive(final FileType fileType, final int braceGroupId) {
        switch (braceGroupId) {
            case XML_TAG_TOKEN_GROUP:
                return fileType == XmlFileType.INSTANCE;
            default:
                return false;
        }
    }

    private static boolean findEndTagStart(HighlighterIterator iterator) {
        IElementType tokenType = (IElementType)iterator.getTokenType();
        int balance = 0;
        int count = 0;
        while (balance >= 0) {
            iterator.retreat();
            count++;
            if (iterator.atEnd()) {
                break;
            }
            tokenType = (IElementType)iterator.getTokenType();
            if (tokenType == XmlTokenType.XML_TAG_END || tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END) {
                balance++;
            }
            else if (tokenType == XmlTokenType.XML_END_TAG_START || tokenType == XmlTokenType.XML_START_TAG_START) {
                balance--;
            }
        }
        while (count-- > 0) iterator.advance();
        return tokenType == XmlTokenType.XML_END_TAG_START;
    }

    private boolean isEndOfSingleHtmlTag(CharSequence text, HighlighterIterator iterator) {
        String tagName = getTagName(text, iterator);
        return tagName != null && HtmlUtil.isSingleHtmlTag(tagName);
    }

    public String getTagName(CharSequence fileText, HighlighterIterator iterator) {
        final IElementType tokenType = (IElementType)iterator.getTokenType();
        String name = null;
        if (tokenType == XmlTokenType.XML_START_TAG_START) {
            iterator.advance();
            IElementType tokenType1 = iterator.atEnd() ? null : (IElementType)iterator.getTokenType();

            boolean wasWhiteSpace = false;
            if (isWhitespace(tokenType1)) {
                wasWhiteSpace = true;
                iterator.advance();
                tokenType1 = iterator.atEnd() ? null : (IElementType)iterator.getTokenType();
            }

            if (tokenType1 == XmlTokenType.XML_TAG_NAME ||
                tokenType1 == XmlTokenType.XML_NAME
            ) {
                name = fileText.subSequence(iterator.getStart(), iterator.getEnd()).toString();
            }

            if (wasWhiteSpace) {
                iterator.retreat();
            }
            iterator.retreat();
        }
        else if (tokenType == XmlTokenType.XML_TAG_END || tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END) {
            int balance = 0;
            int count = 0;
            IElementType tokenType1 = (IElementType)iterator.getTokenType();
            while (balance >= 0) {
                iterator.retreat();
                count++;
                if (iterator.atEnd()) {
                    break;
                }
                tokenType1 = (IElementType)iterator.getTokenType();

                if (tokenType1 == XmlTokenType.XML_TAG_END || tokenType1 == XmlTokenType.XML_EMPTY_ELEMENT_END) {
                    balance++;
                }
                else if (tokenType1 == XmlTokenType.XML_TAG_NAME) {
                    balance--;
                }
            }
            if (tokenType1 == XmlTokenType.XML_TAG_NAME) {
                name = fileText.subSequence(iterator.getStart(), iterator.getEnd()).toString();
            }
            while (count-- > 0) iterator.advance();
        }

        return name;
    }

    protected boolean isWhitespace(final IElementType tokenType1) {
        return tokenType1 == TokenType.WHITE_SPACE;
    }

    public IElementType getOppositeBraceTokenType(@Nonnull final IElementType type) {
        PairedBraceMatcher matcher = PairedBraceMatcher.forLanguage(type.getLanguage());
        if (matcher != null) {
            BracePair[] pairs = matcher.getPairs();
            for (BracePair pair : pairs) {
                if (pair.getLeftBraceType() == type) {
                    return pair.getRightBraceType();
                }
                if (pair.getRightBraceType() == type) {
                    return pair.getLeftBraceType();
                }
            }
        }
        return null;
    }

    @Override
    public int getCodeConstructStart(PsiFile psiFile, int openOffset) {
        return openOffset;
    }
}
