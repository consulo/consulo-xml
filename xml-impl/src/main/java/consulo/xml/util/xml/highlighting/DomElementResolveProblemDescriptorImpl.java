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
package consulo.xml.util.xml.highlighting;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.document.util.UnfairTextRange;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.path.FileReference;
import consulo.util.lang.Pair;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.util.xml.GenericDomValue;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
class DomElementResolveProblemDescriptorImpl extends DomElementProblemDescriptorImpl implements DomElementResolveProblemDescriptor {
  @Nonnull
  private final PsiReference myReference;

  @RequiredReadAction
  public DomElementResolveProblemDescriptorImpl(@Nonnull final GenericDomValue domElement,
                                                @Nonnull final PsiReference reference,
                                                LocalQuickFix... quickFixes) {
    super(domElement,
          reference instanceof FileReference ? ProblemsHolder.unresolvedReferenceMessage(reference).get()
            : ProblemsHolder.unresolvedReferenceMessage(reference).get(),
          HighlightSeverity.ERROR,
          quickFixes);
    myReference = reference;
  }

  @Nonnull
  public PsiReference getPsiReference() {
    return myReference;
  }

  @Nonnull
  public GenericDomValue getDomElement() {
    return (GenericDomValue)super.getDomElement();
  }

  @Nonnull
  protected Pair<TextRange, PsiElement> computeProblemRange() {
    final PsiReference reference = myReference;
    PsiElement element = reference.getElement();
    if (element instanceof XmlAttributeValue && element.getTextLength() == 0) return NO_PROBLEM;

    final TextRange referenceRange = reference.getRangeInElement();
    if (referenceRange.isEmpty()) {
      int startOffset = referenceRange.getStartOffset();
      return element instanceof XmlAttributeValue
        ? Pair.create((TextRange)new UnfairTextRange(startOffset - 1, startOffset + 1), element)
        : Pair.create(TextRange.from(startOffset, 1), element);
    }
    return Pair.create(referenceRange, element);
  }
}
