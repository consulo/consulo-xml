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

/*
 * @author max
 */
package consulo.xml.lang.html;

import com.intellij.xml.util.HtmlUtil;
import consulo.language.ast.CustomParsingType;
import consulo.language.ast.IElementType;
import consulo.language.ast.ILazyParseableElementType;
import consulo.language.codeStyle.IndentHelperExtension;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.parser.PsiBuilder;
import consulo.localize.LocalizeValue;
import consulo.util.collection.Stack;
import consulo.util.lang.StringUtil;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class HtmlParsing {
  private static final String TR_TAG = "tr";
  private static final String TD_TAG = "td";
  private static final String DD_TAG = "dd";
  private static final String DT_TAG = "dt";
  private static final String TABLE_TAG = "table";

  private final PsiBuilder myBuilder;
  private final Stack<String> myTagNamesStack = new Stack<>();
  private final Stack<PsiBuilder.Marker> myTagMarkersStack = new Stack<>();
  private static final String COMPLETION_NAME = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.toLowerCase();

  public HtmlParsing(final PsiBuilder builder) {
    myBuilder = builder;
  }

  public void parseDocument() {
    final PsiBuilder.Marker document = mark();

    while (token() == XmlTokenType.XML_COMMENT_START) {
      parseComment();
    }

    parseProlog();

    PsiBuilder.Marker error = null;
    while (!eof()) {
      final IElementType tt = token();
      if (tt == XmlTokenType.XML_START_TAG_START) {
        error = flushError(error);
        parseTag("");
        myTagMarkersStack.clear();
        myTagNamesStack.clear();
      } else if (tt == XmlTokenType.XML_COMMENT_START) {
        error = flushError(error);
        parseComment();
      } else if (tt == XmlTokenType.XML_PI_START) {
        error = flushError(error);
        parseProcessingInstruction();
      } else if (tt == XmlTokenType.XML_REAL_WHITE_SPACE || tt == XmlTokenType.XML_CHAR_ENTITY_REF || tt == XmlTokenType.XML_DATA_CHARACTERS) {
        error = flushError(error);
        advance();
      } else if (tt == XmlTokenType.XML_END_TAG_START) {
        final PsiBuilder.Marker tagEndError = myBuilder.mark();

        advance();
        if (token() == XmlTokenType.XML_NAME) {
          advance();
          if (token() == XmlTokenType.XML_TAG_END) {
            advance();
          }
        }

        tagEndError.error(XmlErrorLocalize.xmlParsingClosingTagMatchesNothing());
      } else {
        if (error == null) error = mark();
        advance();
      }
    }

    if (error != null) {
      error.error(XmlErrorLocalize.topLevelElementIsNotCompleted());
    }

    document.done(XmlElementType.HTML_DOCUMENT);
  }

  @Nullable
  private static PsiBuilder.Marker flushError(PsiBuilder.Marker error) {
    if (error != null) {
      error.error(XmlErrorLocalize.xmlParsingUnexpectedTokens());
      error = null;
    }
    return error;
  }

  private void parseDoctype() {
    assert token() == XmlTokenType.XML_DOCTYPE_START : "Doctype start expected";
    final PsiBuilder.Marker doctype = mark();
    advance();

    while (token() != XmlTokenType.XML_DOCTYPE_END && !eof()) advance();
    if (eof()) {
      error(XmlErrorLocalize.xmlParsingUnexpectedEndOfFile());
    } else {
      advance();
    }

    doctype.done(XmlElementType.XML_DOCTYPE);
  }

  private static boolean ddordt(String name) {
    return DT_TAG.equals(name) || DD_TAG.equals(name);
  }

  private boolean parseTag(String parentName) {
    assert token() == XmlTokenType.XML_START_TAG_START : "Tag start expected";
    final PsiBuilder.Marker tag = mark();
    myTagMarkersStack.push(tag);

    // Start tag header
    advance();
    final String originalTagName;
    if (token() != XmlTokenType.XML_NAME) {
      error(XmlErrorLocalize.xmlParsingTagNameExpected());
      originalTagName = "";
    } else {
      originalTagName = myBuilder.getTokenText();
      advance();
    }

    String tagName = StringUtil.toLowerCase(originalTagName);
    if (ddordt(tagName) && ddordt(parentName) ||
        tagName.equals(parentName) && HtmlUtil.isOptionalEndForHtmlTagL(tagName) ||
        myTagMarkersStack.size() > MAGIC_FRAME_COUNT // no chance for evil guys wanting us to have stack overflow
    ) {
      tag.rollbackTo();
      myTagMarkersStack.pop();
      return false;
    }

    myTagNamesStack.push(tagName);

    boolean freeMakerTag = !tagName.isEmpty() && '#' == tagName.charAt(0);

    do {
      final IElementType tt = token();
      if (freeMakerTag) {
        if (tt == XmlTokenType.XML_EMPTY_ELEMENT_END ||
            tt == XmlTokenType.XML_TAG_END ||
            tt == XmlTokenType.XML_END_TAG_START ||
            tt == XmlTokenType.XML_START_TAG_START) break;
        advance();
      } else {
        if (tt == XmlTokenType.XML_NAME) {
          parseAttribute();
        } else if (tt == XmlTokenType.XML_CHAR_ENTITY_REF || tt == XmlTokenType.XML_ENTITY_REF_TOKEN) {
          parseReference();
        } else {
          break;
        }
      }
    }
    while (!eof());

    if (token() == XmlTokenType.XML_EMPTY_ELEMENT_END) {
      advance();
      tag.done(XmlElementType.HTML_TAG);
      return true;
    }

    if (token() == XmlTokenType.XML_TAG_END) {
      advance();
    } else {
      error(XmlErrorLocalize.tagStartIsNotClosed());
      tag.done(XmlElementType.HTML_TAG);
      return true;
    }

    if (HtmlUtil.isSingleHtmlTagL(tagName)) {
      final PsiBuilder.Marker footer = mark();
      if (token() == XmlTokenType.XML_END_TAG_START) {
        advance();
        if (token() == XmlTokenType.XML_NAME) {
          if (tagName.equalsIgnoreCase(myBuilder.getTokenText())) {
            advance();
            footer.drop();
            if (token() == XmlTokenType.XML_TAG_END) {
              advance();
            }
            tag.done(XmlElementType.HTML_TAG);
            return true;
          }
        }
      }

      footer.rollbackTo();
      tag.done(XmlElementType.HTML_TAG);
      return true;
    }

    // Done header, start content

    boolean isInlineTagContainer = HtmlUtil.isInlineTagContainerL(tagName);
    boolean isOptionalTagEnd = HtmlUtil.isOptionalEndForHtmlTagL(tagName);

    PsiBuilder.Marker firstBlockChild = null;

    PsiBuilder.Marker xmlText = null;
    while (!eof()) {
      final IElementType tt = token();
      if (tt == XmlTokenType.XML_START_TAG_START) {
        xmlText = terminateText(xmlText);
        if (!parseTag(tagName)) {
          tag.done(XmlElementType.HTML_TAG);
          return true;
        }

        PsiBuilder.Marker childMarker = myTagMarkersStack.pop();
        String childName = myTagNamesStack.pop();

        if (isOptionalTagEnd) {
          boolean foundMatch = childTerminatesParentInStack(childName, true);
          if (foundMatch) {
            myTagMarkersStack.pop();
            myTagNamesStack.pop();

            myTagMarkersStack.push(childMarker);
            myTagNamesStack.push(childName);

            tag.doneBefore(XmlElementType.HTML_TAG, childMarker);
            return true;
          }
        }


        if (isInlineTagContainer && HtmlUtil.isHtmlBlockTagL(childName) && isOptionalTagEnd && !HtmlUtil.isPossiblyInlineTag(childName)) {
          tag.doneBefore(XmlElementType.HTML_TAG, childMarker);
          return true;
        } else if (isOptionalTagEnd && firstBlockChild == null && HtmlUtil.isHtmlBlockTagL(childName) && !HtmlUtil.isHtmlBlockTagL(tagName) && canTerminate(childName, tagName)) {
          firstBlockChild = childMarker;
        }
      } else if (tt == XmlTokenType.XML_PI_START) {
        xmlText = terminateText(xmlText);
        parseProcessingInstruction();
      } else if (tt == XmlTokenType.XML_ENTITY_REF_TOKEN) {
        xmlText = terminateText(xmlText);
        parseReference();
      } else if (tt == XmlTokenType.XML_CHAR_ENTITY_REF) {
        xmlText = startText(xmlText);
        parseReference();
      } else if (tt == XmlTokenType.XML_CDATA_START) {
        xmlText = startText(xmlText);
        parseCData();
      } else if (tt == XmlTokenType.XML_COMMENT_START) {
        xmlText = startText(xmlText);
        parseComment();
      } else if (tt == XmlTokenType.XML_BAD_CHARACTER) {
        xmlText = startText(xmlText);
        final PsiBuilder.Marker error = mark();
        advance();
        error.error(XmlErrorLocalize.unescapedAmpersandOrNonterminatedCharacterEntityReference());
      } else if (tt instanceof CustomParsingType || tt instanceof ILazyParseableElementType) {
        xmlText = terminateText(xmlText);
        advance();
      } else if (token() == XmlTokenType.XML_END_TAG_START) {
        xmlText = terminateText(xmlText);
        final PsiBuilder.Marker footer = mark();
        advance();

        if (token() == XmlTokenType.XML_NAME) {
          String endName = StringUtil.toLowerCase(myBuilder.getTokenText());
          if (!tagName.equals(endName) && !endName.endsWith(COMPLETION_NAME)) {
            final boolean hasChancesToMatch = HtmlUtil.isOptionalEndForHtmlTagL(endName) ? childTerminatesParentInStack(endName, false) : myTagNamesStack.contains(endName);
            if (hasChancesToMatch) {
              footer.rollbackTo();
              if (isOptionalTagEnd) {
                if (firstBlockChild != null) {
                  tag.doneBefore(XmlElementType.HTML_TAG, firstBlockChild);
                } else {
                  tag.done(XmlElementType.HTML_TAG);
                }
              } else {
                error(XmlErrorLocalize.namedElementIsNotClosed(originalTagName));
                tag.done(XmlElementType.HTML_TAG);
              }
              return true;
            } else {
              advance();
              if (token() == XmlTokenType.XML_TAG_END) advance();
              footer.error(XmlErrorLocalize.xmlParsingClosingTagMatchesNothing());
              continue;
            }
          }

          advance();

          while (token() != XmlTokenType.XML_TAG_END && token() != XmlTokenType.XML_START_TAG_START && token() != XmlTokenType.XML_END_TAG_START && !eof()) {
            error(XmlErrorLocalize.xmlParsingUnexpectedToken());
            advance();
          }
        } else {
          error(XmlErrorLocalize.xmlParsingClosingTagNameMissing());
        }
        footer.drop();

        if (token() == XmlTokenType.XML_TAG_END) {
          advance();
        } else {
          error(XmlErrorLocalize.xmlParsingClosingTagIsNotDone());
        }

        tag.done(XmlElementType.HTML_TAG);
        return true;
      } else {
        xmlText = startText(xmlText);
        advance();
      }
    }

    terminateText(xmlText);

    if (isOptionalTagEnd || "body".equalsIgnoreCase(tagName) || "html".equalsIgnoreCase(tagName)) {
      if (firstBlockChild != null) {
        tag.doneBefore(XmlElementType.HTML_TAG, firstBlockChild);
      } else {
        tag.done(XmlElementType.HTML_TAG);
      }
    } else {
      error(XmlErrorLocalize.namedElementIsNotClosed(originalTagName));
      tag.done(XmlElementType.HTML_TAG);
    }

    return true;
  }

  private static boolean canTerminate(final String childTagName, final String tagName) {
    // TODO: make hash
    return !(tagName.equalsIgnoreCase(TR_TAG) && childTagName.equalsIgnoreCase(TD_TAG)) ||
        tagName.equalsIgnoreCase(TABLE_TAG) && childTagName.equalsIgnoreCase(TR_TAG);
  }

  private boolean childTerminatesParentInStack(final String childName, final boolean terminateOnNonOptionalTag) {
    boolean isTD = TD_TAG.equals(childName);
    boolean isTR = TR_TAG.equals(childName);

    for (int i = myTagNamesStack.size() - 1; i >= 0; i--) {
      String parentName = myTagNamesStack.get(i);
      if (terminateOnNonOptionalTag && !HtmlUtil.isOptionalEndForHtmlTagL(parentName)) return false;
      if (isTD && (TR_TAG.equals(parentName) || TABLE_TAG.equals(parentName)) ||
          isTR && TABLE_TAG.equals(parentName)) {
        return false;
      }

      if (childName.equals(parentName)) {
        return true;
      }
    }
    return false;
  }


  @Nonnull
  private PsiBuilder.Marker startText(@Nullable PsiBuilder.Marker xmlText) {
    if (xmlText == null) {
      xmlText = mark();
      assert xmlText != null;
    }
    return xmlText;
  }

  protected final PsiBuilder.Marker mark() {
    return myBuilder.mark();
  }

  @Nullable
  private static PsiBuilder.Marker terminateText(@Nullable PsiBuilder.Marker xmlText) {
    if (xmlText != null) {
      xmlText.done(XmlElementType.XML_TEXT);
      xmlText = null;
    }
    return xmlText;
  }

  private void parseCData() {
    assert token() == XmlTokenType.XML_CDATA_START;
    final PsiBuilder.Marker cdata = mark();
    while (token() != XmlTokenType.XML_CDATA_END && !eof()) {
      advance();
    }

    if (!eof()) {
      advance();
    }

    cdata.done(XmlElementType.XML_CDATA);
  }

  protected void parseComment() {
    final PsiBuilder.Marker comment = mark();
    advance();
    while (true) {
      final IElementType tt = token();
      if (tt == XmlTokenType.XML_COMMENT_CHARACTERS || tt == XmlTokenType.XML_CHAR_ENTITY_REF || tt == XmlTokenType.XML_CONDITIONAL_COMMENT_START
          || tt == XmlTokenType.XML_CONDITIONAL_COMMENT_START_END || tt == XmlTokenType.XML_CONDITIONAL_COMMENT_END_START
          || tt == XmlTokenType.XML_CONDITIONAL_COMMENT_END) {
        advance();
        continue;
      }
      if (tt == XmlTokenType.XML_BAD_CHARACTER) {
        final PsiBuilder.Marker error = mark();
        advance();
        error.error(XmlErrorLocalize.xmlParsingBadCharacter());
        continue;
      }
      if (tt == XmlTokenType.XML_COMMENT_END) {
        advance();
      }
      break;
    }
    comment.done(XmlElementType.XML_COMMENT);
  }

  private void parseReference() {
    if (token() == XmlTokenType.XML_CHAR_ENTITY_REF) {
      advance();
    } else if (token() == XmlTokenType.XML_ENTITY_REF_TOKEN) {
      final PsiBuilder.Marker ref = mark();
      advance();
      ref.done(XmlElementType.XML_ENTITY_REF);
    } else {
      assert false : "Unexpected token";
    }
  }

  private void parseAttribute() {
    assert token() == XmlTokenType.XML_NAME;
    final PsiBuilder.Marker att = mark();
    advance();
    if (token() == XmlTokenType.XML_EQ) {
      advance();
      parseAttributeValue();
      att.done(XmlElementType.XML_ATTRIBUTE);
    } else {
      att.done(XmlElementType.XML_ATTRIBUTE);
    }
  }

  private void parseAttributeValue() {
    final PsiBuilder.Marker attValue = mark();
    if (token() == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER) {
      while (true) {
        final IElementType tt = token();
        if (tt == null || tt == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER || tt == XmlTokenType.XML_END_TAG_START || tt == XmlTokenType
            .XML_EMPTY_ELEMENT_END ||
            tt == XmlTokenType.XML_START_TAG_START) {
          break;
        }

        if (tt == XmlTokenType.XML_BAD_CHARACTER) {
          final PsiBuilder.Marker error = mark();
          advance();
          error.error(XmlErrorLocalize.unescapedAmpersandOrNonterminatedCharacterEntityReference());
        } else if (tt == XmlTokenType.XML_ENTITY_REF_TOKEN) {
          parseReference();
        } else {
          advance();
        }
      }

      if (token() == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
        advance();
      } else {
        error(XmlErrorLocalize.xmlParsingUnclosedAttributeValue());
      }
    } else {
      if (token() != XmlTokenType.XML_TAG_END && token() != XmlTokenType.XML_EMPTY_ELEMENT_END) {
        advance(); // Single token att value
      }
    }

    attValue.done(XmlElementType.XML_ATTRIBUTE_VALUE);
  }

  private void parseProlog() {
    while (true) {
      final IElementType tt = token();
      if (tt == XmlTokenType.XML_COMMENT_START) {
        parseComment();
      } else if (tt == XmlTokenType.XML_REAL_WHITE_SPACE) {
        advance();
      } else {
        break;
      }
    }

    final PsiBuilder.Marker prolog = mark();
    while (true) {
      final IElementType tt = token();
      if (tt == XmlTokenType.XML_PI_START) {
        parseProcessingInstruction();
      } else if (tt == XmlTokenType.XML_DOCTYPE_START) {
        parseDoctype();
      } else if (tt == XmlTokenType.XML_COMMENT_START) {
        parseComment();
      } else if (tt == XmlTokenType.XML_REAL_WHITE_SPACE) {
        advance();
      } else {
        break;
      }
    }
    prolog.done(XmlElementType.XML_PROLOG);
  }

  private void parseProcessingInstruction() {
    assert token() == XmlTokenType.XML_PI_START;
    final PsiBuilder.Marker pi = mark();
    advance();
    if (token() == XmlTokenType.XML_NAME || token() == XmlTokenType.XML_PI_TARGET) {
      advance();
    }

    while (token() == XmlTokenType.XML_NAME) {
      advance();
      if (token() == XmlTokenType.XML_EQ) {
        advance();
      } else {
        error(XmlErrorLocalize.expectedAttributeEqSign());
      }
      parseAttributeValue();
    }

    if (token() == XmlTokenType.XML_PI_END) {
      advance();
    } else {
      error(XmlErrorLocalize.xmlParsingUnterminatedProcessingInstruction());
    }

    pi.done(XmlElementType.XML_PROCESSING_INSTRUCTION);
  }

  protected final IElementType token() {
    return myBuilder.getTokenType();
  }

  protected final boolean eof() {
    return myBuilder.eof();
  }

  protected final void advance() {
    myBuilder.advanceLexer();
  }

  private void error(final LocalizeValue message) {
    myBuilder.error(message);
  }

  private static final int MAGIC_FRAME_COUNT = IndentHelperExtension.TOO_BIG_WALK_THRESHOLD + (int) (Math.pow(Math.E, Math.PI) * Math.sin(Math.random()));
}
