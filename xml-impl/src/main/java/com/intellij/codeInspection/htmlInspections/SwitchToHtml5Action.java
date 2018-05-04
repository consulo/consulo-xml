/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.codeInspection.htmlInspections;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.XmlErrorMessages;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.javaee.ExternalResourceManagerEx;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xml.Html5SchemaProvider;

/**
 * @author Eugene.Kudelevsky
 */
public class SwitchToHtml5Action implements LocalQuickFix, IntentionAction {

  @Nonnull
  @Override
  public String getName() {
    return XmlErrorMessages.message("switch.to.html5.quickfix.text");
  }

  @Nonnull
  @Override
  public String getText() {
    return getFamilyName();
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return getName();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    applyFix(project);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @Override
  public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    applyFix(project);
  }

  private static void applyFix(Project project) {
    ExternalResourceManagerEx.getInstanceEx().setDefaultHtmlDoctype(Html5SchemaProvider.getHtml5SchemaLocation(), project);
    DaemonCodeAnalyzer.getInstance(project).restart();
  }
}
