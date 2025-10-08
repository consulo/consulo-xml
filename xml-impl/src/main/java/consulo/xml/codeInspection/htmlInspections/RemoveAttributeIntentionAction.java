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
import consulo.application.Result;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlAttribute;

import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
public class RemoveAttributeIntentionAction implements LocalQuickFix {
    private final String myLocalName;

    public RemoveAttributeIntentionAction(String localName) {
        myLocalName = localName;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return XmlErrorLocalize.removeAttributeQuickfixText(myLocalName);
    }

    @Override
    @RequiredUIAccess
    public void applyFix(@Nonnull final Project project, @Nonnull ProblemDescriptor descriptor) {
        PsiElement e = descriptor.getPsiElement();
        final XmlAttribute myAttribute = PsiTreeUtil.getParentOfType(e, XmlAttribute.class);
      if (myAttribute == null) {
        return;
      }

        if (!FileModificationService.getInstance().prepareFileForWrite(myAttribute.getContainingFile())) {
            return;
        }

        new WriteCommandAction(project) {
            @Override
            @RequiredWriteAction
            protected void run(Result result) throws Throwable {
                myAttribute.delete();
            }
        }.execute();
    }
}
