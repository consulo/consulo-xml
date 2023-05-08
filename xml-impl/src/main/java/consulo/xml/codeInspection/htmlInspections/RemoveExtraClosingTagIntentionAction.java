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

import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.ast.ASTNode;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.xml.codeInsight.daemon.XmlErrorMessages;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;

import javax.annotation.Nonnull;

/**
 * @author spleaner
 */
public class RemoveExtraClosingTagIntentionAction implements LocalQuickFix, SyntheticIntentionAction {
  @Override
  @Nonnull
  public String getFamilyName() {
    return XmlErrorMessages.message("remove.extra.closing.tag.quickfix");
  }

  @Override
  @Nonnull
  public String getName() {
    return XmlErrorMessages.message("remove.extra.closing.tag.quickfix");
  }


  @Override
  @Nonnull
  public String getText() {
    return getName();
  }

  @Override
  public boolean isAvailable(@Nonnull final Project project, final Editor editor, final PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    final int offset = editor.getCaretModel().getOffset();
    final PsiElement psiElement = file.findElementAt(offset);
    if (psiElement == null || !psiElement.isValid() || !(psiElement instanceof XmlToken)) {
      return;
    }

    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
    doFix(psiElement);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  private static void doFix(@Nonnull final PsiElement element) throws consulo.language.util.IncorrectOperationException {
    final XmlToken endNameToken = (XmlToken)element;
    final PsiElement tagElement = endNameToken.getParent();
    if (!(tagElement instanceof XmlTag) && !(tagElement instanceof PsiErrorElement)) return;

    if (tagElement instanceof PsiErrorElement) {
      tagElement.delete();
    }
    else {
      final ASTNode astNode = tagElement.getNode();
      if (astNode != null) {
        final ASTNode endTagStart = XmlChildRole.CLOSING_TAG_START_FINDER.findChild(astNode);
        if (endTagStart != null) {
          final Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(tagElement.getContainingFile());
          if (document != null) {
            document.deleteString(endTagStart.getStartOffset(), tagElement.getLastChild().getTextRange().getEndOffset());
          }
        }
      }
    }
  }

  @Override
  public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
    final PsiElement element = descriptor.getPsiElement();
    if (!element.isValid() || !(element instanceof XmlToken)) return;
    if (!FileModificationService.getInstance().prepareFileForWrite(element.getContainingFile())) return;

    new WriteCommandAction(project) {
      @Override
      protected void run(final Result result) throws Throwable {
        doFix(element);
      }
    }.execute();
  }
}
