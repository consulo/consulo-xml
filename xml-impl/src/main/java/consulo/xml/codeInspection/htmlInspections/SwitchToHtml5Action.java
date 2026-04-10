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

import com.intellij.xml.Html5SchemaProvider;
import consulo.codeEditor.Editor;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.xml.localize.XmlErrorLocalize;
import consulo.xml.javaee.ExternalResourceManagerEx;


/**
 * @author Eugene.Kudelevsky
 */
public class SwitchToHtml5Action implements LocalQuickFix, IntentionAction {
    @Override
    public LocalizeValue getName() {
        return XmlErrorLocalize.switchToHtml5QuickfixText();
    }

    @Override
    public LocalizeValue getText() {
        return getName();
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        applyFix(project);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        applyFix(project);
    }

    private static void applyFix(Project project) {
        ExternalResourceManagerEx.getInstanceEx().setDefaultHtmlDoctype(Html5SchemaProvider.getHtml5SchemaLocation(), project);
        DaemonCodeAnalyzer.getInstance(project).restart();
    }
}
