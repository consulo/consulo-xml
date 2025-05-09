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
import consulo.language.impl.psi.CompositePsiElement;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.action.ExtendWordSelectionHandlerBase;

import java.util.List;

@ExtensionImpl
public class XmlCDATAContentSelectioner extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(PsiElement e) {
        return e instanceof CompositePsiElement compositePsiElement
            && compositePsiElement.getElementType() == XmlElementType.XML_CDATA;
    }

    @Override
    @RequiredReadAction
    public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
        List<TextRange> result = super.select(e, editorText, cursorOffset, editor);
        PsiElement[] children = e.getChildren();

        PsiElement first = null;
        PsiElement last = null;
        for (PsiElement child : children) {
            if (child instanceof XmlToken) {
                XmlToken token = (XmlToken)child;
                if (token.getTokenType() == XmlTokenType.XML_CDATA_START) {
                    first = token.getNextSibling();
                }
                if (token.getTokenType() == XmlTokenType.XML_CDATA_END) {
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

        return result;
    }
}