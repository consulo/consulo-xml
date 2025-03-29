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
package consulo.xml.codeInsight.editorActions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.action.ExtendWordSelectionHandlerBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.xml.psi.xml.*;

import java.util.List;

@ExtensionImpl
public class XmlTagSelectioner extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(PsiElement e) {
        return e instanceof XmlTag;
    }

    @Override
    @RequiredReadAction
    public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
        List<TextRange> result = super.select(e, editorText, cursorOffset, editor);
        PsiElement[] children = e.getChildren();

        addTagContentSelection(children, result, editorText);

        PsiElement prev = e.getPrevSibling();
        while (prev instanceof PsiWhiteSpace || prev instanceof XmlText || prev instanceof XmlComment) {
            if (prev instanceof XmlText xmlText && xmlText.getText().trim().length() > 0) {
                break;
            }
            if (prev instanceof XmlComment) {
                result.addAll(expandToWholeLine(
                    editorText,
                    new TextRange(
                        prev.getTextRange().getStartOffset(),
                        e.getTextRange().getEndOffset()
                    ),
                    false
                ));
            }
            prev = prev.getPrevSibling();
        }

        return result;
    }

    @RequiredReadAction
    private static void addTagContentSelection(PsiElement[] children, List<TextRange> result, CharSequence editorText) {
        PsiElement first = null, last = null;
        for (PsiElement child : children) {
            if (child instanceof XmlToken token) {
                if (token.getTokenType() == XmlTokenType.XML_TAG_END) {
                    first = token.getNextSibling();
                }
                if (token.getTokenType() == XmlTokenType.XML_END_TAG_START) {
                    last = token.getPrevSibling();
                    break;
                }
            }
        }

        if (first != null && last != null) {
            result.addAll(expandToWholeLine(
                editorText,
                new TextRange(
                    first.getTextRange().getStartOffset(),
                    last.getTextRange().getEndOffset()
                ),
                false
            ));
        }
    }
}