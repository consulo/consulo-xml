/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import org.intellij.plugins.relaxNG.compact.psi.*;

import jakarta.annotation.Nonnull;

/**
 * @author sweinreuter
 * @since 2007-08-10
 */
public class ReferenceAnnotator extends RncElementVisitor implements Annotator {
  private AnnotationHolder myHolder;

  @Override
  public void annotate(@Nonnull PsiElement psiElement, @Nonnull AnnotationHolder holder) {
    myHolder = holder;
    try {
      psiElement.accept(this);
    } finally {
      myHolder = null;
    }
  }

  @Override
  public void visitInclude(RncInclude include) {
    checkReferences(include.getReferences());
  }

  @Override
  public void visitExternalRef(RncExternalRef ref) {
    checkReferences(ref.getReferences());
  }

  @Override
  public void visitRef(RncRef pattern) {
    checkReferences(pattern.getReferences());
  }

  @Override
  public void visitParentRef(RncParentRef pattern) {
    checkReferences(pattern.getReferences());
  }

  @Override
  public void visitName(RncName name) {
    checkReferences(name.getReferences());
  }

  private void checkReferences(PsiReference[] references) {
    for (PsiReference reference : references) {
      if (!reference.isSoft()) {
        if (reference.resolve() == null) {
          if (reference instanceof PsiPolyVariantReference) {
            final PsiPolyVariantReference pvr = (PsiPolyVariantReference)reference;
            if (pvr.multiResolve(false).length == 0) {
              addError(reference);
            }
          } else {
            addError(reference);
          }
        }
      }
    }
  }

  @RequiredReadAction
  private void addError(PsiReference reference) {
    final TextRange rangeInElement = reference.getRangeInElement();
    final TextRange range = TextRange.from(reference.getElement().getTextRange().getStartOffset()
            + rangeInElement.getStartOffset(), rangeInElement.getLength());

    LocalizeValue message = ProblemsHolder.unresolvedReferenceMessage(reference);
    final Annotation annotation = myHolder.createErrorAnnotation(range, message.get());

    annotation.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);

    if (reference instanceof LocalQuickFixProvider) {
      LocalQuickFix[] fixes = ((LocalQuickFixProvider)reference).getQuickFixes();
      if (fixes != null) {
        InspectionManager inspectionManager = InspectionManager.getInstance(reference.getElement().getProject());
        for (LocalQuickFix fix : fixes) {
          ProblemDescriptor descriptor = inspectionManager.createProblemDescriptor(reference.getElement(), annotation.getMessage().get(), fix,
                                                                                   ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, true);
          annotation.registerFix(fix, null, null, descriptor);
        }
      }
    }
  }
}
