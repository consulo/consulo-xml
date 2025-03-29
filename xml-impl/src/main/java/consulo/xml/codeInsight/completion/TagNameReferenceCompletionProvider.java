/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.impl.source.xml.TagNameReference;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlTagNameProvider;
import consulo.ide.impl.idea.codeInsight.completion.LegacyCompletionContributor;
import consulo.language.ast.ASTNode;
import consulo.language.editor.completion.AutoCompletionPolicy;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.*;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class TagNameReferenceCompletionProvider implements CompletionProvider {
    public static LookupElement[] getTagNameVariants(@Nonnull XmlTag tag, String prefix) {
        List<LookupElement> elements = new ArrayList<>();
        for (XmlTagNameProvider tagNameProvider : XmlTagNameProvider.EP_NAME.getExtensionList()) {
            tagNameProvider.addTagNameVariants(elements, tag, prefix);
        }
        return elements.toArray(new LookupElement[elements.size()]);
    }

    @Override
    @RequiredReadAction
    public void addCompletions(@Nonnull CompletionParameters parameters, ProcessingContext context, @Nonnull CompletionResultSet result) {
        LegacyCompletionContributor.processReferences(
            parameters,
            result,
            (reference, set) ->
            {
                if (reference instanceof TagNameReference tagNameReference) {
                    collectCompletionVariants(tagNameReference, set);
                }
                else if (reference instanceof SchemaPrefixReference schemaPrefixReference) {
                    TagNameReference tagNameReference = schemaPrefixReference.getTagNameReference();
                    if (tagNameReference != null && !tagNameReference.isStartTagFlag()) {
                        set.accept(createClosingTagLookupElement(
                            (XmlTag)tagNameReference.getElement(),
                            true,
                            tagNameReference.getNameElement()
                        ));
                    }
                }
            }
        );
    }

    public static void collectCompletionVariants(TagNameReference tagNameReference, Consumer<LookupElement> consumer) {
        PsiElement element = tagNameReference.getElement();
        if (element instanceof XmlTag tag) {
            if (!tagNameReference.isStartTagFlag()) {
                consumer.accept(createClosingTagLookupElement((XmlTag)element, false, tagNameReference.getNameElement()));
            }
            else {
                for (LookupElement variant : getTagNameVariants(tag, tag.getNamespacePrefix())) {
                    consumer.accept(variant);
                }
            }
        }
    }

    public static LookupElement createClosingTagLookupElement(XmlTag tag, boolean includePrefix, ASTNode nameElement) {
        LookupElementBuilder builder = LookupElementBuilder.create(
            includePrefix || !nameElement.getText().contains(":") ? tag.getName() : tag.getLocalName()
        );
        return LookupElementDecorator.withInsertHandler(
            TailTypeDecorator.withTail(
                AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE.applyPolicy(builder),
                TailType.createSimpleTailType('>')
            ),
            XmlClosingTagInsertHandler.INSTANCE
        );
    }
}
