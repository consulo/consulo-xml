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

import javax.annotation.Nonnull;

import consulo.language.editor.FileModificationService;
import consulo.xml.codeInsight.daemon.XmlErrorMessages;
import consulo.application.Result;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;

/**
 * @author spleaner
 */
public class RemoveAttributeIntentionAction implements LocalQuickFix {
  private final String myLocalName;

  public RemoveAttributeIntentionAction(final String localName) {
    myLocalName = localName;
  }

  @Override
  @Nonnull
  public String getName() {
    return XmlErrorMessages.message("remove.attribute.quickfix.text", myLocalName);
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return XmlErrorMessages.message("remove.attribute.quickfix.family");
  }

  @Override
  public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
    PsiElement e = descriptor.getPsiElement();
    final XmlAttribute myAttribute = PsiTreeUtil.getParentOfType(e, XmlAttribute.class);
    if (myAttribute == null) return;

    if (!FileModificationService.getInstance().prepareFileForWrite(myAttribute.getContainingFile())) {
      return;
    }

    new WriteCommandAction(project) {
      @Override
      protected void run(final Result result) throws Throwable {
        myAttribute.delete();
      }
    }.execute();
  }
}
