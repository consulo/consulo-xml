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
package com.intellij.xml.actions.validate;

import javax.annotation.Nonnull;
import com.intellij.ide.highlighter.XHtmlFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;

/**
 * @author mike
 */
public class ValidateXmlAction extends AnAction {
  private static final Key<String> runningValidationKey = Key.create("xml.running.validation.indicator");

  public ValidateXmlAction() {
  }

  private ValidateXmlActionHandler getHandler(final @Nonnull PsiFile file) {
    ValidateXmlActionHandler handler = new ValidateXmlActionHandler(true);
    handler.setErrorReporter(
      new StdErrorReporter(handler, file.getProject(),
                           new Runnable() {
                             public void run() {
                               doRunAction(file);
                             }
                           }
      )
    );
    return handler;
  }

  public void actionPerformed(AnActionEvent e) {
    final PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (psiFile != null) {
      doRunAction(psiFile);
    }
  }

  private void doRunAction(final @Nonnull PsiFile psiFile) {

    CommandProcessor.getInstance().executeCommand(psiFile.getProject(), new Runnable(){
      public void run(){
        final Runnable action = new Runnable() {
          public void run() {
            try {
              psiFile.putUserData(runningValidationKey, "");
              PsiDocumentManager.getInstance(psiFile.getProject()).commitAllDocuments();

              getHandler(psiFile).doValidate((XmlFile)psiFile);
            }
            finally {
              psiFile.putUserData(runningValidationKey, null);
            }
          }
        };
        ApplicationManager.getApplication().runWriteAction(action);
      }
    },
                                                  getCommandName(),
                                                  null
    );
  }

  private String getCommandName(){
    String text = getTemplatePresentation().getText();
    return text != null ? text : "";
  }

  public void update(AnActionEvent event) {
    super.update(event);

    Presentation presentation = event.getPresentation();
    PsiElement psiElement = event.getData(CommonDataKeys.PSI_FILE);

    boolean flag = psiElement instanceof XmlFile;
    presentation.setVisible(flag);
    boolean value = psiElement instanceof XmlFile;

    if (value) {
      final PsiFile containingFile = psiElement.getContainingFile();

      if (containingFile!=null &&
          (containingFile.getFileType() == XmlFileType.INSTANCE ||
           containingFile.getFileType() == XHtmlFileType.INSTANCE
          )) {
        value = containingFile.getUserData(runningValidationKey) == null;
      } else {
        value = false;
      }
    }

    presentation.setEnabled(value);
    if (ActionPlaces.isPopupPlace(event.getPlace())) {
      presentation.setVisible(value);
    }
  }
}
