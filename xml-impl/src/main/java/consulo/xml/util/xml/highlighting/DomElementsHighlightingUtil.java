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

import consulo.codeEditor.CodeInsightColors;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * User: Sergey.Vasiliev
 */
public class DomElementsHighlightingUtil {
  private DomElementsHighlightingUtil() {
  }

  @Nullable
  public static ProblemDescriptor createProblemDescriptors(final InspectionManager manager, final DomElementProblemDescriptor problemDescriptor) {
    final ProblemHighlightType type = getProblemHighlightType(problemDescriptor);
    return createProblemDescriptors(problemDescriptor, new Function<Pair<TextRange, PsiElement>, ProblemDescriptor>() {
      public ProblemDescriptor apply(final Pair<TextRange, PsiElement> s) {
        return manager
          .createProblemDescriptor(s.second, s.first, problemDescriptor.getDescriptionTemplate(), type, true, problemDescriptor.getFixes());
      }
    });
  }

  // TODO: move it to DomElementProblemDescriptorImpl
  private static ProblemHighlightType getProblemHighlightType(final DomElementProblemDescriptor problemDescriptor) {
    if (problemDescriptor.getHighlightType() != null) {
      return problemDescriptor.getHighlightType();
    }
    if (problemDescriptor instanceof DomElementResolveProblemDescriptor) {
      final TextRange range = ((DomElementResolveProblemDescriptor)problemDescriptor).getPsiReference().getRangeInElement();
      if (range.getStartOffset() != range.getEndOffset()) {
        return ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
      }
    }
    return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
  }

  @Nullable
  public static Annotation createAnnotation(final DomElementProblemDescriptor problemDescriptor) {

    return createProblemDescriptors(problemDescriptor, new Function<Pair<TextRange, PsiElement>, Annotation>() {
      public Annotation apply(final Pair<TextRange, PsiElement> s) {
        String text = problemDescriptor.getDescriptionTemplate();
        if (StringUtil.isEmpty(text)) text = null;
        final HighlightSeverity severity = problemDescriptor.getHighlightSeverity();

        TextRange range = s.first;
        if (text == null) range = TextRange.from(range.getStartOffset(), 0);
        range = range.shiftRight(s.second.getTextRange().getStartOffset());
        final Annotation annotation = createAnnotation(severity, range, text);

        if (problemDescriptor instanceof DomElementResolveProblemDescriptor) {
          annotation.setTextAttributes(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
        }

        for(LocalQuickFix fix:problemDescriptor.getFixes()) {
          if (fix instanceof IntentionAction) annotation.registerFix((IntentionAction)fix);
        }
        return annotation;
      }
    });
  }

  private static Annotation createAnnotation(final HighlightSeverity severity,
                                             final TextRange range,
                                             final String text) {
    String tooltip = text == null ? null : "<html><body>" + XmlStringUtil.escapeString(text) + "</body></html>";
    return new Annotation(range.getStartOffset(), range.getEndOffset(), severity, text, tooltip);
  }

  @Nullable
  private static <T> T createProblemDescriptors(final DomElementProblemDescriptor problemDescriptor,
                                                      final Function<Pair<TextRange, PsiElement>, T> creator) {

    final Pair<TextRange, PsiElement> range = ((DomElementProblemDescriptorImpl)problemDescriptor).getProblemRange();
    return range == DomElementProblemDescriptorImpl.NO_PROBLEM ? null : creator.apply(range);
  }

}
