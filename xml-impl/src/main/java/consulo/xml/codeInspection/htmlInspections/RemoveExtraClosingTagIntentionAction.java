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
package consulo.xml.codeInspection.htmlInspections;

import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.ast.ASTNode;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
public class RemoveExtraClosingTagIntentionAction implements LocalQuickFix, SyntheticIntentionAction {
    @Nonnull
    @Override
    public LocalizeValue getName() {
        return XmlErrorLocalize.removeExtraClosingTagQuickfix();
    }

    @Nonnull
    @Override
    public LocalizeValue getText() {
        return getName();
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    @RequiredWriteAction
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        int offset = editor.getCaretModel().getOffset();
        PsiElement psiElement = file.findElementAt(offset);
        if (psiElement == null || !psiElement.isValid() || !(psiElement instanceof XmlToken)) {
            return;
        }

        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        doFix(psiElement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @RequiredWriteAction
    private static void doFix(@Nonnull PsiElement element) throws consulo.language.util.IncorrectOperationException {
        XmlToken endNameToken = (XmlToken) element;
        PsiElement tagElement = endNameToken.getParent();
        if (!(tagElement instanceof XmlTag) && !(tagElement instanceof PsiErrorElement)) {
            return;
        }

        if (tagElement instanceof PsiErrorElement) {
            tagElement.delete();
        }
        else {
            ASTNode astNode = tagElement.getNode();
            if (astNode != null) {
                ASTNode endTagStart = XmlChildRole.CLOSING_TAG_START_FINDER.findChild(astNode);
                if (endTagStart != null) {
                    Document document =
                        PsiDocumentManager.getInstance(element.getProject()).getDocument(tagElement.getContainingFile());
                    if (document != null) {
                        document.deleteString(endTagStart.getStartOffset(), tagElement.getLastChild().getTextRange().getEndOffset());
                    }
                }
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        if (!element.isValid() || !(element instanceof XmlToken)) {
            return;
        }
        if (!FileModificationService.getInstance().prepareFileForWrite(element.getContainingFile())) {
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .inWriteAction()
            .run(() -> doFix(element));
    }
}
