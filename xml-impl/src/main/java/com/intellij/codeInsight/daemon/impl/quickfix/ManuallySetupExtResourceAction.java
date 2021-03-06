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
package com.intellij.codeInsight.daemon.impl.quickfix;

import javax.annotation.Nonnull;

import com.intellij.javaee.MapExternalResourceDialog;
import com.intellij.javaee.ExternalResourceManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

/**
 * @author mike
 */
public class ManuallySetupExtResourceAction extends BaseExtResourceAction {

  protected String getQuickFixKeyId() {
    return "manually.setup.external.resource";
  }

  protected void doInvoke(@Nonnull final PsiFile file, final int offset, @Nonnull final String uri, final Editor editor) throws IncorrectOperationException {
    final MapExternalResourceDialog dialog = new MapExternalResourceDialog(uri, file.getProject(), file, null);
    dialog.show();
    if (dialog.isOK()) {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          String location = dialog.getResourceLocation();
          ExternalResourceManager.getInstance().addResource(dialog.getUri(), location);
        }
      });
    }
  }

  public boolean startInWriteAction() {
    return false;
  }
}
