// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xml.actions.validate;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.Application;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.xml.ide.highlighter.XHtmlFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.xml.XmlFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author mike
 */
@ActionImpl(
    id = "ValidateXml",
    parents = {
        @ActionParentRef(@ActionRef(id = "EditorPopupMenu.Run")),
        @ActionParentRef(@ActionRef(id = "ProjectViewPopupMenuRefactoringGroup")),
        @ActionParentRef(@ActionRef(id = "EditorTabPopupMenuEx"))
    }
)
public class ValidateXmlAction extends AnAction {
    private static final Key<String> runningValidationKey = Key.create("xml.running.validation.indicator");

    private final Application myApplication;

    @Inject
    public ValidateXmlAction(Application application) {
        super(XmlLocalize.actionValidateXmlText(), XmlLocalize.actionValidateXmlDescription());
        myApplication = application;
    }

    private ValidateXmlHandler getHandler(@Nonnull PsiFile file) {
        ValidateXmlHandler handler = myApplication.getExtensionPoint(ValidateXmlHandler.class)
            .findFirstSafe(thisHandler -> thisHandler.isAvailable((XmlFile) file));
        if (handler == null) {
            ValidateXmlActionHandler validateHandler = new ValidateXmlActionHandler(true);
            validateHandler.setErrorReporter(new StdErrorReporter(validateHandler, file, () -> doRunAction(file)));
            handler = validateHandler;
        }
        return handler;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        PsiFile psiFile = e.getData(PsiFile.KEY);
        if (psiFile != null && psiFile.getVirtualFile() != null) {
            doRunAction(psiFile);
        }
    }

    private void doRunAction(@Nonnull PsiFile psiFile) {
        CommandProcessor.getInstance().newCommand()
            .project(psiFile.getProject())
            .name(LocalizeValue.ofNullable(getCommandName()))
            .inWriteAction()
            .run(() -> {
                try {
                    psiFile.putUserData(runningValidationKey, "");
                    PsiDocumentManager.getInstance(psiFile.getProject()).commitAllDocuments();

                    getHandler(psiFile).doValidate((XmlFile) psiFile);
                }
                finally {
                    psiFile.putUserData(runningValidationKey, null);
                }
            });
    }

    private String getCommandName() {
        String text = getTemplatePresentation().getText();
        return text != null ? text : "";
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        PsiElement psiElement = event.getData(PsiFile.KEY);

        boolean enabled = psiElement instanceof XmlFile;
        presentation.setVisible(enabled);

        if (enabled) {
            PsiFile containingFile = psiElement.getContainingFile();

            enabled = containingFile != null && containingFile.getVirtualFile() != null
                && (containingFile.getFileType() == XmlFileType.INSTANCE || containingFile.getFileType() == XHtmlFileType.INSTANCE)
                && containingFile.getUserData(runningValidationKey) == null;
        }

        presentation.setEnabledAndVisible(enabled);
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            presentation.setVisible(enabled);
        }
    }
}
