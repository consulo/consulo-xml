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
package consulo.xml.codeInspection.htmlInspections;

import javax.annotation.Nonnull;

import consulo.codeEditor.Editor;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.xml.codeInsight.daemon.XmlErrorMessages;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.xml.javaee.ExternalResourceManagerEx;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import com.intellij.xml.Html5SchemaProvider;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.util.IncorrectOperationException;

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
