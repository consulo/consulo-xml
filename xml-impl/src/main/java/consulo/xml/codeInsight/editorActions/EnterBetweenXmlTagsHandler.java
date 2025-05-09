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
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.ast.IElementType;
import consulo.language.editor.action.EnterHandlerDelegateAdapter;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.ref.SimpleReference;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "xmlEnter")
public class EnterBetweenXmlTagsHandler extends EnterHandlerDelegateAdapter {
    @Override
    @RequiredReadAction
    public Result preprocessEnter(
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull SimpleReference<Integer> caretOffset,
        @Nonnull SimpleReference<Integer> caretAdvance,
        @Nonnull DataContext dataContext,
        EditorActionHandler originalHandler
    ) {
        Project project = dataContext.getData(Project.KEY);

        if (file instanceof XmlFile && isBetweenXmlTags(project, editor, file, caretOffset.get())) {
            originalHandler.execute(editor, dataContext);
            return Result.DefaultForceIndent;
        }
        return Result.Continue;
    }

    @RequiredReadAction
    private static boolean isBetweenXmlTags(Project project, Editor editor, PsiFile file, int offset) {
        if (offset == 0) {
            return false;
        }
        CharSequence chars = editor.getDocument().getCharsSequence();
        if (chars.charAt(offset - 1) != '>') {
            return false;
        }

        EditorHighlighter highlighter = editor.getHighlighter();
        HighlighterIterator iterator = highlighter.createIterator(offset - 1);
        if (iterator.getTokenType() != XmlTokenType.XML_TAG_END) {
            return false;
        }

        if (isAtTheEndOfEmptyTag(project, editor, file, iterator)) {
            return false;
        }

        iterator.retreat();

        int retrieveCount = 1;
        while (!iterator.atEnd()) {
            IElementType tokenType = (IElementType)iterator.getTokenType();
            if (tokenType == XmlTokenType.XML_END_TAG_START) {
                return false;
            }
            if (tokenType == XmlTokenType.XML_START_TAG_START) {
                break;
            }
            ++retrieveCount;
            iterator.retreat();
        }

        for (int i = 0; i < retrieveCount; ++i) iterator.advance();
        iterator.advance();
        return !iterator.atEnd() && iterator.getTokenType() == XmlTokenType.XML_END_TAG_START;
    }

    @RequiredReadAction
    private static boolean isAtTheEndOfEmptyTag(Project project, Editor editor, PsiFile file, HighlighterIterator iterator) {
        if (iterator.getTokenType() != XmlTokenType.XML_TAG_END) {
            return false;
        }

        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        PsiElement element = file.findElementAt(iterator.getStart());

        if (element == null) {
            return false;
        }

        PsiElement parent = element.getParent();
        return parent instanceof XmlTag && parent.getTextRange().getEndOffset() == iterator.getEnd();
    }
}
