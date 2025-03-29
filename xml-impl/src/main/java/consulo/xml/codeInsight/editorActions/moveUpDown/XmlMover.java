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
package consulo.xml.codeInsight.editorActions.moveUpDown;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.document.util.UnfairTextRange;
import consulo.language.editor.moveUpDown.LineMover;
import consulo.language.editor.moveUpDown.LineRange;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.impl.source.xml.TagNameVariantCollector;
import consulo.xml.psi.impl.source.xml.XmlDocumentImpl;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "xml", order = "before line")
class XmlMover extends LineMover {
    @Inject
    XmlMover() {
    }

    @Override
    @RequiredReadAction
    public boolean checkAvailable(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull MoveInfo info, boolean down) {
        if (!(file instanceof XmlFile)) {
            return false;
        }
        if (!super.checkAvailable(editor, file, info, down)) {
            return false;
        }

        // updated moved range end to cover multiline tag start
        Document document = editor.getDocument();
        int movedLineStart = document.getLineStartOffset(info.toMove.startLine);
        int movedLineEnd = document.getLineEndOffset(info.toMove.endLine - 1);

        PsiElement movedEndElement = file.findElementAt(movedLineEnd);
        if (movedEndElement instanceof PsiWhiteSpace whiteSpace) {
            movedEndElement = PsiTreeUtil.prevLeaf(whiteSpace);
        }
        PsiElement movedStartElement = file.findElementAt(movedLineStart);
        if (movedStartElement instanceof PsiWhiteSpace whiteSpace) {
            movedStartElement = PsiTreeUtil.nextLeaf(whiteSpace);
        }

        if (movedEndElement == null || movedStartElement == null) {
            return false;
        }
        PsiNamedElement namedParentAtEnd = PsiTreeUtil.getParentOfType(movedEndElement, PsiNamedElement.class);
        PsiNamedElement namedParentAtStart = PsiTreeUtil.getParentOfType(movedStartElement, PsiNamedElement.class);

        if (checkInjections(movedEndElement, movedStartElement)) {
            return false;
        }

        XmlTag nearestTag = PsiTreeUtil.getParentOfType(movedStartElement, XmlTag.class);
        if (nearestTag != null &&
            ("script".equals(nearestTag.getLocalName()) ||
                (nearestTag instanceof HtmlTag && "script".equalsIgnoreCase(nearestTag.getLocalName()))
            )
        ) {
            return false;
        }

        PsiNamedElement movedParent = null;

        if (namedParentAtEnd == namedParentAtStart) {
            movedParent = namedParentAtEnd;
        }
        else if (namedParentAtEnd instanceof XmlAttribute attributeAtEnd
            && namedParentAtStart instanceof XmlTag tagAtStart
            && attributeAtEnd.getParent() == tagAtStart) {
            movedParent = tagAtStart;
        }
        else if (namedParentAtStart instanceof XmlAttribute attribute
            && namedParentAtEnd instanceof XmlTag tagAtEnd
            && attribute.getParent() == tagAtEnd) {
            movedParent = tagAtEnd;
        }

        if (movedParent == null) {
            return false;
        }

        TextRange textRange = movedParent.getTextRange();

        if (movedParent instanceof XmlTag tag) {
            PsiElement parent = tag.getParent();
            if (!(parent instanceof XmlTag) && PsiTreeUtil.getChildrenOfType(parent, XmlTag.class).length < 2) {
                // the only top-level tag
                return info.prohibitMove();
            }
            TextRange valueRange = tag.getValue().getTextRange();
            int valueStart = valueRange.getStartOffset();

            if (movedLineStart < valueStart && valueStart + 1 < document.getTextLength()) {
                movedLineStart = updateMovedRegionEnd(document, movedLineStart, valueStart + 1, info, down);
            }
            if (movedLineStart < valueStart) {
                movedLineStart = updateMovedRegionStart(document, movedLineStart, tag.getTextRange().getStartOffset(), info, down);
            }
        }
        else if (movedParent instanceof XmlAttribute) {
            int endOffset = textRange.getEndOffset() + 1;
            if (endOffset < document.getTextLength()) {
                movedLineStart = updateMovedRegionEnd(document, movedLineStart, endOffset, info, down);
            }
            movedLineStart = updateMovedRegionStart(document, movedLineStart, textRange.getStartOffset(), info, down);
        }

        TextRange moveDestinationRange = new UnfairTextRange(
            document.getLineStartOffset(info.toMove2.startLine),
            document.getLineEndOffset(info.toMove2.endLine - 1)
        );

        if (movedParent instanceof XmlAttribute) {
            XmlTag parent = ((XmlAttribute)movedParent).getParent();

            if (parent != null) {
                TextRange valueRange = parent.getValue().getTextRange();

                // Do not move attributes out of tags
                if ((down && moveDestinationRange.getEndOffset() >= valueRange.getStartOffset()) ||
                    (!down && moveDestinationRange.getStartOffset() <= parent.getTextRange().getStartOffset())
                ) {
                    return info.prohibitMove();
                }
            }
        }

        if (down) {
            PsiElement updatedElement = file.findElementAt(moveDestinationRange.getEndOffset());
            if (updatedElement instanceof PsiWhiteSpace whiteSpace) {
                updatedElement = PsiTreeUtil.prevLeaf(whiteSpace);
            }

            if (updatedElement != null) {
                PsiNamedElement targetParent = PsiTreeUtil.getParentOfType(updatedElement, movedParent.getClass());

                if (targetParent instanceof XmlTag targetTag) {
                    if (targetParent == movedParent) {
                        return false;
                    }
                    if (moveTags(info, (XmlTag)movedParent, targetTag, down)) {
                        return true;
                    }

                    int offset = targetTag.isEmpty()
                        ? targetTag.getTextRange().getStartOffset()
                        : targetTag.getValue().getTextRange().getStartOffset();
                    updatedMovedIntoEnd(document, info, offset);
                    if (targetTag.isEmpty()) {
                        info.toMove2 = new LineRange(targetParent);
                    }
                }
                else if (targetParent instanceof XmlAttribute) {
                    updatedMovedIntoEnd(document, info, targetParent.getTextRange().getEndOffset());
                }
            }
        }
        else {
            PsiElement updatedElement = file.findElementAt(moveDestinationRange.getStartOffset());
            if (updatedElement instanceof PsiWhiteSpace whiteSpace) {
                updatedElement = PsiTreeUtil.nextLeaf(whiteSpace);
            }

            if (updatedElement != null) {
                PsiNamedElement targetParent = PsiTreeUtil.getParentOfType(updatedElement, movedParent.getClass());

                if (targetParent instanceof XmlTag targetTag) {
                    TextRange tagValueRange = targetTag.getValue().getTextRange();

                    // We need to update destination range to jump over tag start
                    XmlTag[] subtags = targetTag.getSubTags();
                    if ((tagValueRange.contains(movedLineStart) && subtags.length > 0 && subtags[0] == movedParent)
                        || (tagValueRange.getLength() == 0 && targetTag.getTextRange().intersects(moveDestinationRange))
                    ) {
                        int line = document.getLineNumber(targetTag.getTextRange().getStartOffset());
                        LineRange toMove2 = info.toMove2;
                        info.toMove2 = new LineRange(Math.min(line, toMove2.startLine), toMove2.endLine);
                    }
                    if (targetParent == movedParent) {
                        return false;
                    }
                    if (moveTags(info, (XmlTag)movedParent, targetTag, down)) {
                        return true;
                    }

                }
                else if (targetParent instanceof XmlAttribute) {
                    int line = document.getLineNumber(targetParent.getTextRange().getStartOffset());
                    LineRange toMove2 = info.toMove2;
                    info.toMove2 = new LineRange(Math.min(line, toMove2.startLine), toMove2.endLine);
                }
            }
        }

        if (movedParent instanceof XmlTag) {
            // it's quite simple after all...
            info.toMove = new LineRange(movedParent);
        }
        return true;
    }

    @RequiredReadAction
    private static boolean moveTags(MoveInfo info, XmlTag moved, XmlTag target, boolean down) {
        if (target.getParent() == moved) {
            // we are going to jump into our own children
            // this can mean that target computed incorrectly
            XmlTag next = down
                ? PsiTreeUtil.getNextSiblingOfType(moved, XmlTag.class)
                : PsiTreeUtil.getPrevSiblingOfType(moved, XmlTag.class);
            if (next == null) {
                return info.prohibitMove();
            }
            info.toMove = new LineRange(moved);
            info.toMove2 = new LineRange(next);
            return true;
        }
        else if (moved.getParent() == target) {
            return false;
        }

        LineRange targetRange = new LineRange(target);
        if (targetRange.contains(info.toMove2)) {
            // we are going to jump into sibling tag
            XmlElementDescriptor descriptor = moved.getDescriptor();
            if (descriptor == null) {
                return false;
            }
            XmlNSDescriptor nsDescriptor = descriptor.getNSDescriptor();
            if (nsDescriptor == null) {
                return false;
            }
            XmlFile descriptorFile = nsDescriptor.getDescriptorFile();
            if (descriptorFile == null || XmlDocumentImpl.isAutoGeneratedSchema(descriptorFile)) {
                return false;
            }
            if (!TagNameVariantCollector.couldContain(target, moved)) {
                info.toMove = new LineRange(moved);
                info.toMove2 = targetRange;
                return true;
            }
        }

        return false;
    }

    private static boolean checkInjections(PsiElement movedEndElement, PsiElement movedStartElement) {
        XmlText text = PsiTreeUtil.getParentOfType(movedStartElement, XmlText.class);
        XmlText text2 = PsiTreeUtil.getParentOfType(movedEndElement, XmlText.class);

        // Let's do not care about injections for this mover
        return (text != null && InjectedLanguageManager.getInstance(text.getProject()).getInjectedPsiFiles(text) != null)
            || (text2 != null && InjectedLanguageManager.getInstance(text2.getProject()).getInjectedPsiFiles(text2) != null);
    }

    private static void updatedMovedIntoEnd(Document document, @Nonnull MoveInfo info, int offset) {
        if (offset + 1 < document.getTextLength()) {
            int line = document.getLineNumber(offset + 1);
            LineRange toMove2 = info.toMove2;
            if (toMove2 == null) {
                return;
            }
            info.toMove2 = new LineRange(toMove2.startLine, Math.min(Math.max(line, toMove2.endLine), document.getLineCount() - 1));
        }
    }

    private static int updateMovedRegionStart(
        Document document,
        int movedLineStart,
        int offset,
        @Nonnull MoveInfo info,
        boolean down
    ) {
        int line = document.getLineNumber(offset);
        LineRange toMove = info.toMove;
        int delta = toMove.startLine - line;
        info.toMove = new LineRange(Math.min(line, toMove.startLine), toMove.endLine);

        // update moved range
        if (delta > 0 && !down) {
            LineRange toMove2 = info.toMove2;
            info.toMove2 = new LineRange(toMove2.startLine - delta, toMove2.endLine - delta);
            movedLineStart = document.getLineStartOffset(toMove.startLine);
        }
        return movedLineStart;
    }

    private static int updateMovedRegionEnd(
        Document document,
        int movedLineStart,
        int valueStart,
        @Nonnull MoveInfo info,
        boolean down
    ) {
        int line = document.getLineNumber(valueStart);
        LineRange toMove = info.toMove;
        int delta = line - toMove.endLine;
        info.toMove = new LineRange(toMove.startLine, Math.max(line, toMove.endLine));

        // update moved range
        if (delta > 0 && down) {
            LineRange toMove2 = info.toMove2;
            info.toMove2 = new LineRange(
                toMove2.startLine + delta,
                Math.min(toMove2.endLine + delta, document.getLineCount() - 1)
            );
            movedLineStart = document.getLineStartOffset(toMove.startLine);
        }
        return movedLineStart;
    }
}