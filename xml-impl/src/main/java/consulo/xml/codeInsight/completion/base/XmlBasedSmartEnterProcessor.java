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
package consulo.xml.codeInsight.completion.base;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.action.SmartEnterProcessor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.CharArrayUtil;
import consulo.xml.psi.xml.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
public abstract class XmlBasedSmartEnterProcessor extends SmartEnterProcessor {
    private static final Logger LOG = Logger.getInstance(XmlBasedSmartEnterProcessor.class);

    public boolean process(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull final PsiFile psiFile) {
        return completeEndTag(project, editor, psiFile);
    }

    private boolean completeEndTag(Project project, Editor editor, PsiFile psiFile) {
        final PsiElement atCaret = getStatementAtCaret(editor, psiFile);
        XmlTag tagAtCaret = PsiTreeUtil.getParentOfType(atCaret, XmlTag.class);
        if (tagAtCaret == null) {
            return false;
        }
        try {
            final ASTNode emptyTagEnd = XmlChildRole.EMPTY_TAG_END_FINDER.findChild(tagAtCaret.getNode());
            final ASTNode endTagEnd = XmlChildRole.START_TAG_END_FINDER.findChild(tagAtCaret.getNode());
            final Document doc = editor.getDocument();
            if (emptyTagEnd == null && endTagEnd == null) {
                int insertionOffset = tagAtCaret.getTextRange().getEndOffset();
                int caretAt = editor.getCaretModel().getOffset();
                final CharSequence text = doc.getCharsSequence();
                final int probableCommaOffset = CharArrayUtil.shiftForward(text, insertionOffset, " \t");
                final PsiElement siebling = tagAtCaret.getNextSibling();
                int caretTo = caretAt;
                char ch;

                if (caretAt < probableCommaOffset) {
                    final XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(atCaret, XmlAttribute.class, false, XmlTag.class);

                    CharSequence tagNameText = null;
                    if (xmlAttribute != null) {
                        final ASTNode node = tagAtCaret.getNode();
                        if (node != null) {
                            final ASTNode tagName = XmlChildRole.START_TAG_NAME_FINDER.findChild(node);
                            if (tagName != null) {
                                tagNameText = tagName.getText();
                            }
                        }

                        final XmlAttributeValue valueElement = xmlAttribute.getValueElement();
                        final TextRange textRange = xmlAttribute.getTextRange();
                        caretAt = valueElement == null
                            ? textRange.getStartOffset()
                            : getClosingQuote(xmlAttribute).length() == 0 ? textRange.getEndOffset() : caretAt;
                    }

                    if (tagNameText == null) {
                        tagNameText = text.subSequence(tagAtCaret.getTextRange().getStartOffset() + 1, caretAt);
                    }

                    final PsiElement element = psiFile.findElementAt(probableCommaOffset);
                    final XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
                    boolean shouldInsertClosingTag =
                        shouldAfterWrapTextWithTag(caretAt, probableCommaOffset) || shouldInsertClosingTag(xmlAttribute, tagAtCaret);
                    final CharSequence text2insert = getClosingPart(xmlAttribute, tagAtCaret, !shouldInsertClosingTag);

                    if (tag != null && tag.getTextRange().getStartOffset() == probableCommaOffset) {
                        doc.insertString(caretAt, text2insert);
                        if (shouldInsertClosingTag) {
                            doc.insertString(
                                tag.getTextRange().getEndOffset() + text2insert.length(),
                                "</" + tagAtCaret.getName() + ">"
                            );
                        }

                        caretTo = tag.getTextRange().getEndOffset() + text2insert.length();
                    }
                    else {
                        doc.insertString(caretAt, text2insert);
                        if (shouldInsertClosingTag) {
                            doc.insertString(probableCommaOffset + text2insert.length(), "</" + tagNameText + ">");
                        }

                        caretTo = probableCommaOffset + text2insert.length();
                    }
                }
                else if (siebling instanceof XmlTag && siebling.getTextRange().getStartOffset() == caretAt) {
                    final XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(atCaret, XmlAttribute.class, false, XmlTag.class);
                    final CharSequence text2insert = getClosingPart(xmlAttribute, tagAtCaret, false);

                    doc.insertString(caretAt, text2insert);
                    if (shouldInsertClosingTag(xmlAttribute, tagAtCaret)) {
                        doc.insertString(
                            siebling.getTextRange().getEndOffset() + text2insert.length(),
                            "</" + tagAtCaret.getName() + ">"
                        );
                    }

                    caretTo = siebling.getTextRange().getEndOffset() + text2insert.length();
                }
                else if (probableCommaOffset >= text.length() || ((ch = text.charAt(probableCommaOffset)) != '/' && ch != '>')) {
                    final XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(atCaret, XmlAttribute.class, false, XmlTag.class);
                    final CharSequence text2insert = getClosingPart(xmlAttribute, tagAtCaret, true);

                    doc.insertString(insertionOffset, text2insert);
                    caretTo = insertionOffset + text2insert.length();
                }

                commitChanges(project, editor, psiFile, caretTo, null);

                return true;
            }
            else {
                final XmlTag unclosedTag = findClosestUnclosedTag(tagAtCaret);
                if (unclosedTag == null) {
                    return false;
                }

                final String closingTagString = "</" + unclosedTag.getName() + ">";

                final XmlTag parentTag = unclosedTag.getParentTag();
                final ASTNode parentEndTagNode =
                    parentTag != null ? XmlChildRole.CLOSING_TAG_START_FINDER.findChild(parentTag.getNode()) : null;
                final int offset = parentEndTagNode != null
                    ? parentEndTagNode.getTextRange().getStartOffset()
                    : unclosedTag.getTextRange().getEndOffset();

                doc.insertString(offset, closingTagString);
                commitChanges(project, editor, psiFile, offset, parentTag != null ? parentTag : unclosedTag);
                return true;
            }
        }
        catch (IncorrectOperationException e) {
            LOG.error(e);
        }
        return false;
    }

    protected boolean shouldAfterWrapTextWithTag(int caretAt, int probableCommaOffset) {
        return probableCommaOffset > caretAt;
    }

    private void commitChanges(Project project, Editor editor, PsiFile psiFile, int caretOffset, @Nullable XmlTag tagToReformat) {
        if (isUncommited(project)) {
            commit(editor);
            if (tagToReformat == null) {
                tagToReformat = PsiTreeUtil.getParentOfType(getStatementAtCaret(editor, psiFile), XmlTag.class);
            }
            editor.getCaretModel().moveToOffset(caretOffset);
        }
        if (tagToReformat != null) {
            reformat(tagToReformat);
        }
        commit(editor);
    }

    @Nullable
    private static XmlTag findClosestUnclosedTag(final XmlTag tag) {
        XmlTag unclosedTag = tag;
        while (unclosedTag != null) {
            final PsiElement lastChild = unclosedTag.getLastChild();
            if (isTagUnclosed(lastChild)) {
                return unclosedTag;
            }
            final XmlTag prevTag = PsiTreeUtil.getPrevSiblingOfType(unclosedTag, XmlTag.class);
            unclosedTag = prevTag != null ? prevTag : PsiTreeUtil.getParentOfType(unclosedTag, XmlTag.class);
        }
        return null;
    }

    protected static boolean isTagUnclosed(PsiElement lastChild) {
        //strange approach, but it's universal for xml and html
        return lastChild != null &&
            lastChild.getNode().getElementType() != XmlTokenType.XML_TAG_END &&
            lastChild.getNode().getElementType() != XmlTokenType.XML_EMPTY_ELEMENT_END;
    }

    protected boolean shouldInsertClosingTag(final XmlAttribute xmlAttribute, final XmlTag tagAtCaret) {
        return xmlAttribute == null || getClosingQuote(xmlAttribute).length() != 0;
    }

    protected String getClosingPart(final XmlAttribute xmlAttribute, final XmlTag tagAtCaret, final boolean emptyTag) {
        return getClosingQuote(xmlAttribute) + (emptyTag ? "/>" : ">");
    }

    @Nonnull
    protected static CharSequence getClosingQuote(@Nullable final XmlAttribute attribute) {
        if (attribute == null) {
            return "";
        }

        final XmlAttributeValue element = attribute.getValueElement();
        if (element == null) {
            return "";
        }

        final String s = element.getText();
        if (s != null && s.length() > 0) {
            if (s.charAt(0) == '"' && s.charAt(s.length() - 1) != '"') {
                return "\"";
            }
            else if (s.charAt(0) == '\'' && s.charAt(s.length() - 1) != '\'') {
                return "'";
            }
        }

        return "";
    }
}
