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
package consulo.xml.codeInsight.daemon.impl.quickfix;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.DependentNSReference;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.URLReference;
import consulo.xml.psi.xml.XmlFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author mike
 */
abstract class BaseExtResourceAction implements SyntheticIntentionAction {
    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof XmlFile)) {
            return false;
        }

        int offset = editor.getCaretModel().getOffset();
        String uri = findUri(file, offset);
        return uri != null && isAcceptableUri(uri);
    }

    protected boolean isAcceptableUri(String uri) {
        return true;
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        int offset = editor.getCaretModel().getOffset();

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        String uri = findUri(file, offset);
        if (uri == null) {
            return;
        }

        doInvoke(file, offset, uri, editor);
    }

    @RequiredUIAccess
    protected abstract void doInvoke(@Nonnull PsiFile file, int offset, @Nonnull String uri, Editor editor)
        throws IncorrectOperationException;

    @Nullable
    @RequiredReadAction
    public static String findUri(PsiFile file, int offset) {
        PsiReference currentRef = file.getViewProvider().findReferenceAt(offset, file.getLanguage());
        if (currentRef == null) {
            currentRef = file.getViewProvider().findReferenceAt(offset);
        }
        if (currentRef instanceof URLReference || currentRef instanceof DependentNSReference) {
            return currentRef.getCanonicalText();
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ManuallySetupExtResourceAction;
    }
}