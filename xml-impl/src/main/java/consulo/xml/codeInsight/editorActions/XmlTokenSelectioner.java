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

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.editor.action.ExtendWordSelectionHandlerBase;
import consulo.language.editor.action.SelectWordUtil;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class XmlTokenSelectioner extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(PsiElement e) {
        return e instanceof XmlToken && !HtmlSelectioner.canSelectElement(e);
    }

    @Override
    public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
        XmlToken token = (XmlToken)e;

        if (shouldSelectToken(token)) {
            List<TextRange> ranges = super.select(e, editorText, cursorOffset, editor);
            SelectWordUtil.addWordSelection(editor.getSettings().isCamelWords(), editorText, cursorOffset, ranges);
            return ranges;
        }
        else {
            List<TextRange> result = new ArrayList<>();
            SelectWordUtil.addWordSelection(editor.getSettings().isCamelWords(), editorText, cursorOffset, result);
            return result;
        }
    }

    static boolean shouldSelectToken(XmlToken token) {
        IElementType type = token.getTokenType();
        return type != XmlTokenType.XML_DATA_CHARACTERS
            && type != XmlTokenType.XML_START_TAG_START
            && type != XmlTokenType.XML_END_TAG_START
            && type != XmlTokenType.XML_EMPTY_ELEMENT_END
            && type != XmlTokenType.XML_TAG_END;
    }
}