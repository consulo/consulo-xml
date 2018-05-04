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

import com.intellij.javaee.ExternalResourceManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.IncorrectOperationException;

/**
 * @author mike
 */
public class IgnoreExtResourceAction extends BaseExtResourceAction {
  protected String getQuickFixKeyId() {
    return "ignore.external.resource.text";
  }

  protected void doInvoke(@Nonnull final PsiFile file, final int offset, @Nonnull final String uri, final Editor editor) throws IncorrectOperationException {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        ExternalResourceManagerEx.getInstanceEx().addIgnoredResource(uri);
      }
    });
  }
}
