/*
 * Copyright 2013-2026 consulo.io
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

package consulo.xml.codeInsight.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiMultiReference;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceRange;

import java.util.function.BiConsumer;

/**
 * The references at the caret of a completion, each with a result set matching its own prefix - what XML completion
 * needs of the IDE's completion implementation.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
public final class XmlCompletionReferences {
    private XmlCompletionReferences() {
    }

    @RequiredReadAction
    public static void processReferences(CompletionParameters parameters, CompletionResultSet result,
                                         BiConsumer<PsiReference, CompletionResultSet> consumer) {
        int offset = parameters.getOffset();
        PsiReference reference = parameters.getPosition().getContainingFile().findReferenceAt(offset);
        if (reference instanceof PsiMultiReference multiReference) {
            for (PsiReference child : multiReference.getReferences()) {
                processReference(child, offset, result, consumer);
            }
        }
        else if (reference != null) {
            processReference(reference, offset, result, consumer);
        }
    }

    /**
     * Adds the variants of the references at the caret
     *
     * @return whether a variant was added
     */
    @RequiredReadAction
    public static boolean completeReference(CompletionParameters parameters, CompletionResultSet result) {
        boolean[] added = {false};
        processReferences(parameters, result, (reference, resultSet) -> {
            for (Object variant : reference.getVariants()) {
                LookupElement lookupElement = toLookupElement(variant);
                if (lookupElement != null && resultSet.getPrefixMatcher().prefixMatches(lookupElement)) {
                    resultSet.addElement(lookupElement);
                    added[0] = true;
                }
            }
        });
        return added[0];
    }

    @RequiredReadAction
    private static void processReference(PsiReference reference, int offset, CompletionResultSet result,
                                         BiConsumer<PsiReference, CompletionResultSet> consumer) {
        PsiElement element = reference.getElement();
        int offsetInElement = offset - element.getTextRange().getStartOffset();
        if (!ReferenceRange.containsOffsetInElement(reference, offsetInElement)) {
            return;
        }
        TextRange range = reference.getRangeInElement();
        String text = element.getText();
        if (range.getStartOffset() > offsetInElement || offsetInElement > text.length()) {
            return;
        }
        consumer.accept(reference, result.withPrefixMatcher(text.substring(range.getStartOffset(), offsetInElement)));
    }

    private static LookupElement toLookupElement(Object variant) {
        if (variant instanceof LookupElement lookupElement) {
            return lookupElement;
        }
        if (variant instanceof PsiNamedElement named && named.getName() != null) {
            return LookupElementBuilder.create(named);
        }
        if (variant instanceof String name) {
            return LookupElementBuilder.create(name);
        }
        return null;
    }
}
