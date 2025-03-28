/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.xml.codeInsight.completion;

import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementDecorator;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

public class XmlClosingTagInsertHandler implements InsertHandler<LookupElement> {
    public final static XmlClosingTagInsertHandler INSTANCE = new XmlClosingTagInsertHandler();

    private XmlClosingTagInsertHandler() {
    }

    @Override
    @RequiredUIAccess
    public void handleInsert(InsertionContext context, LookupElement item) {
        Editor editor = context.getEditor();
        Document document = editor.getDocument();
        Project project = context.getProject();
        if (item instanceof LookupElementDecorator lookupElementDecorator) {
            lookupElementDecorator.getDelegate().handleInsert(context);
        }
        PsiDocumentManager.getInstance(project).commitDocument(document);
        int lineOffset = document.getLineStartOffset(document.getLineNumber(editor.getCaretModel().getOffset()));
        CodeStyleManager.getInstance(project).adjustLineIndent(document, lineOffset);
    }
}
