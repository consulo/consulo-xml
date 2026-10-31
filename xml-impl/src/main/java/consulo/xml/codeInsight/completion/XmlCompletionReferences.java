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
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.parser.ParserDefinition;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.stub.IdTableBuilding;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.StringUtil;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
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

    /**
     * The words of the file and the values of its other string literals - what the IDE's word completion adds
     */
    @RequiredReadAction
    public static void addWordCompletionVariants(CompletionResultSet result, CompletionParameters parameters, Set<String> excludes) {
        Set<String> realExcludes = new HashSet<>(excludes);
        for (String exclude : excludes) {
            String[] words = exclude.split("[ \\.-]");
            if (words.length > 0 && StringUtil.isNotEmpty(words[0])) {
                realExcludes.add(words[0]);
            }
        }

        PsiElement position = parameters.getPosition();
        CompletionResultSet javaResultSet = result.withPrefixMatcher(CompletionUtilCore.findJavaIdentifierPrefix(parameters));
        CompletionResultSet plainResultSet = result.withPrefixMatcher(CompletionUtilCore.findAlphanumericPrefix(parameters));
        for (String word : getAllWords(position, parameters.getOffset())) {
            if (!realExcludes.contains(word)) {
                LookupElement item = LookupElementBuilder.create(word);
                javaResultSet.addElement(item);
                plainResultSet.addElement(item);
            }
        }

        addValuesFromOtherStringLiterals(result, parameters, realExcludes, position);
    }

    @RequiredReadAction
    private static void addValuesFromOtherStringLiterals(CompletionResultSet result, CompletionParameters parameters,
                                                         Set<String> realExcludes, PsiElement position) {
        ParserDefinition definition = ParserDefinition.forLanguage(position.getLanguage());
        if (definition == null) {
            return;
        }
        ElementPattern<PsiElement> pattern =
            PlatformPatterns.psiElement().withElementType(definition.getStringLiteralElements(position.getLanguageVersion()));
        PsiElement localString = PsiTreeUtil.findFirstParent(position, false, pattern::accepts);
        if (localString == null) {
            return;
        }
        ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(localString);
        if (manipulator == null) {
            return;
        }
        int offset = manipulator.getRangeInElement(localString).getStartOffset();
        PsiFile file = position.getContainingFile();
        CompletionResultSet fullStringResult = result.withPrefixMatcher(
            file.getText().substring(offset + localString.getTextRange().getStartOffset(), parameters.getOffset()));
        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element == localString) {
                    return;
                }
                if (pattern.accepts(element)) {
                    element.accept(new PsiRecursiveElementWalkingVisitor() {
                        @Override
                        public void visitElement(PsiElement each) {
                            String valueText = ElementManipulators.getValueText(each);
                            if (StringUtil.isNotEmpty(valueText) && !realExcludes.contains(valueText)) {
                                fullStringResult.addElement(LookupElementBuilder.create(valueText));
                            }
                        }
                    });
                    return;
                }
                super.visitElement(element);
            }
        });
    }

    @RequiredReadAction
    private static Set<String> getAllWords(PsiElement context, int offset) {
        Set<String> words = new LinkedHashSet<>();
        if (StringUtil.isEmpty(CompletionUtilCore.findJavaIdentifierPrefix(context, offset))) {
            return words;
        }

        CharSequence chars = context.getContainingFile().getViewProvider().getContents();
        IdTableBuilding.scanWords((chars1, charsArray, start, end) -> {
            if (start > offset || offset > end) {
                words.add(chars1.subSequence(start, end).toString());
            }
        }, chars, 0, chars.length());
        return words;
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
