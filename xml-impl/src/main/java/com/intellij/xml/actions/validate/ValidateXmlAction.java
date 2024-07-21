// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xml.actions.validate;

import consulo.application.ApplicationManager;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.xml.ide.highlighter.XHtmlFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.psi.xml.XmlFile;

import javax.annotation.Nonnull;

/**
 * @author mike
 */
public class ValidateXmlAction extends AnAction {
    private static final Key<String> runningValidationKey = Key.create("xml.running.validation.indicator");

    public ValidateXmlAction() {
    }

    private ValidateXmlHandler getHandler(final @Nonnull PsiFile file) {
        for (ValidateXmlHandler handler : ValidateXmlHandler.EP_NAME.getExtensionList()) {
            if (handler.isAvailable((XmlFile)file)) {
                return handler;
            }
        }
        ValidateXmlActionHandler handler = new ValidateXmlActionHandler(true);
        handler.setErrorReporter(new StdErrorReporter(handler, file, () -> doRunAction(file)));
        return handler;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final PsiFile psiFile = e.getData(PsiFile.KEY);
        if (psiFile != null && psiFile.getVirtualFile() != null) {
            doRunAction(psiFile);
        }
    }

    private void doRunAction(final @Nonnull PsiFile psiFile) {

        CommandProcessor.getInstance().executeCommand(
            psiFile.getProject(),
            () -> {
                final Runnable action = () -> {
                    try {
                        psiFile.putUserData(runningValidationKey, "");
                        PsiDocumentManager.getInstance(psiFile.getProject()).commitAllDocuments();

                        getHandler(psiFile).doValidate((XmlFile)psiFile);
                    }
                    finally {
                        psiFile.putUserData(runningValidationKey, null);
                    }
                };
                ApplicationManager.getApplication().runWriteAction(action);
            },
            getCommandName(),
            null
        );
    }

    private String getCommandName() {
        String text = getTemplatePresentation().getText();
        return text != null ? text : "";
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {

        Presentation presentation = event.getPresentation();
        PsiElement psiElement = event.getData(CommonDataKeys.PSI_FILE);

        boolean visible = psiElement instanceof XmlFile;
        presentation.setVisible(visible);
        boolean enabled = psiElement instanceof XmlFile;

        if (enabled) {
            final PsiFile containingFile = psiElement.getContainingFile();

            if (containingFile != null &&
                containingFile.getVirtualFile() != null &&
                (containingFile.getFileType() == XmlFileType.INSTANCE ||
                    containingFile.getFileType() == XHtmlFileType.INSTANCE
                )) {
                enabled = containingFile.getUserData(runningValidationKey) == null;
            }
            else {
                enabled = false;
            }
        }

        presentation.setEnabled(enabled);
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            presentation.setVisible(enabled);
        }
    }
}
