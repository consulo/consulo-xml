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
import consulo.localize.LocalizeValue;
import consulo.util.lang.Pair;
import consulo.util.lang.xml.XmlStringUtil;

import consulo.xml.lang.xml.XMLLanguage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;

/**
 * @author Sergey.Vasiliev
 */
public class DomElementsHighlightingUtil {
    private DomElementsHighlightingUtil() {
    }

    @Nullable
    @RequiredReadAction
    public static ProblemDescriptor createProblemDescriptors(InspectionManager manager, DomElementProblemDescriptor problemDescriptor) {
        ProblemHighlightType type = getProblemHighlightType(problemDescriptor);
        return createProblemDescriptors(
            problemDescriptor,
            s -> manager.newProblemDescriptor(problemDescriptor.getDescriptionTemplate())
                .range(s.second, s.first)
                .highlightType(type)
                .onTheFly(true)
                .withFixes(problemDescriptor.getFixes())
                .create()
        );
    }

    // TODO: move it to DomElementProblemDescriptorImpl
    @RequiredReadAction
    private static ProblemHighlightType getProblemHighlightType(DomElementProblemDescriptor problemDescriptor) {
        if (problemDescriptor.getHighlightType() != null) {
            return problemDescriptor.getHighlightType();
        }
        if (problemDescriptor instanceof DomElementResolveProblemDescriptor domProblemDescriptor) {
            TextRange range = domProblemDescriptor.getPsiReference().getRangeInElement();
            if (range.getStartOffset() != range.getEndOffset()) {
                return ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
            }
        }
        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    }

    @Nullable
    @RequiredReadAction
    public static Annotation createAnnotation(DomElementProblemDescriptor problemDescriptor) {
        return createProblemDescriptors(
            problemDescriptor,
            s -> {
                LocalizeValue text = problemDescriptor.getDescriptionTemplate();
                HighlightSeverity severity = problemDescriptor.getHighlightSeverity();

                TextRange range = s.first;
                if (text == LocalizeValue.empty()) {
                    range = TextRange.from(range.getStartOffset(), 0);
                }
                range = range.shiftRight(s.second.getTextRange().getStartOffset());
                Annotation annotation = createAnnotation(severity, range, text);

                if (problemDescriptor instanceof DomElementResolveProblemDescriptor) {
                    annotation.setTextAttributes(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
                }

                for (LocalQuickFix fix : problemDescriptor.getFixes()) {
                    if (fix instanceof IntentionAction action) {
                        annotation.registerFix(action);
                    }
                }
                return annotation;
            }
        );
    }

    private static Annotation createAnnotation(HighlightSeverity severity, TextRange range, @Nonnull LocalizeValue text) {
        LocalizeValue tooltip = text == LocalizeValue.empty()
            ? LocalizeValue.empty()
            : text.map((localizeManager, string) -> "<html><body>" + XmlStringUtil.escapeString(string) + "</body></html>");
        return new Annotation(range.getStartOffset(), range.getEndOffset(), severity, text, tooltip, XMLLanguage.INSTANCE);
    }

    @Nullable
    @RequiredReadAction
    private static <T> T createProblemDescriptors(
        DomElementProblemDescriptor problemDescriptor,
        @Nonnull @RequiredReadAction Function<Pair<TextRange, PsiElement>, T> creator
    ) {
        Pair<TextRange, PsiElement> range = ((DomElementProblemDescriptorImpl) problemDescriptor).getProblemRange();
        return range == DomElementProblemDescriptorImpl.NO_PROBLEM ? null : creator.apply(range);
    }
}
