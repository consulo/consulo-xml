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
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.impl.source.html.dtd.HtmlAttributeDescriptorImpl;
import consulo.xml.psi.impl.source.xml.XmlAttributeImpl;
import consulo.xml.psi.impl.source.xml.XmlAttributeReference;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.NamespaceAwareXmlAttributeDescriptor;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.util.HtmlUtil;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.util.ProcessingContext;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.language.editor.completion.CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED;

public class XmlAttributeReferenceCompletionProvider implements CompletionProvider {
    private static final Logger LOG = Logger.getInstance(XmlAttributeReferenceCompletionProvider.class);

    @Override
    @RequiredReadAction
    public void addCompletions(@Nonnull CompletionParameters parameters, ProcessingContext context, @Nonnull CompletionResultSet result) {
        PsiReference reference = parameters.getPosition().getContainingFile().findReferenceAt(parameters.getOffset());
        if (reference instanceof XmlAttributeReference xmlAttributeReference) {
            addAttributeReferenceCompletionVariants(xmlAttributeReference, result, null);
        }
    }

    public static void addAttributeReferenceCompletionVariants(
        XmlAttributeReference reference,
        CompletionResultSet result,
        @Nullable InsertHandler<LookupElement> replacementInsertHandler
    ) {
        XmlTag declarationTag = reference.getElement().getParent();
        LOG.assertTrue(declarationTag.isValid());
        XmlElementDescriptor parentDescriptor = declarationTag.getDescriptor();
        if (parentDescriptor != null) {
            XmlAttribute[] attributes = declarationTag.getAttributes();
            XmlAttributeDescriptor[] descriptors = parentDescriptor.getAttributesDescriptors(declarationTag);

            descriptors = HtmlUtil.appendHtmlSpecificAttributeCompletions(declarationTag, descriptors, reference.getElement());

            addVariants(result, attributes, descriptors, reference.getElement(), replacementInsertHandler);
        }
    }

    private static void addVariants(
        CompletionResultSet result,
        XmlAttribute[] attributes,
        XmlAttributeDescriptor[] descriptors,
        XmlAttribute attribute,
        @Nullable InsertHandler<LookupElement> replacementInsertHandler
    ) {
        XmlTag tag = attribute.getParent();
        PsiFile file = tag.getContainingFile();
        XmlExtension extension = XmlExtension.getExtension(file);
        String prefix = attribute.getName().contains(":")
            && ((XmlAttributeImpl)attribute).getRealLocalName().length() > 0
            ? attribute.getNamespacePrefix() + ":"
            : null;

        for (XmlAttributeDescriptor descriptor : descriptors) {
            if (isValidVariant(attribute, descriptor, attributes, extension)) {
                String name = descriptor.getName(tag);

                InsertHandler<LookupElement> insertHandler = XmlAttributeInsertHandler.INSTANCE;

                if (tag instanceof HtmlTag && HtmlUtil.isShortNotationOfBooleanAttributePreferred()
                    && HtmlUtil.isBooleanAttribute(descriptor, tag)) {
                    insertHandler = null;
                }

                if (replacementInsertHandler != null) {
                    insertHandler = replacementInsertHandler;
                }
                else if (descriptor instanceof NamespaceAwareXmlAttributeDescriptor namespaceAwareXmlAttributeDescriptor) {
                    String namespace = namespaceAwareXmlAttributeDescriptor.getNamespace(tag);

                    if (file instanceof XmlFile && namespace != null && namespace.length() > 0
                        && !name.contains(":") && tag.getPrefixByNamespace(namespace) == null) {
                        insertHandler = new XmlAttributeInsertHandler(namespace);
                    }
                }
                if (prefix == null || name.startsWith(prefix)) {
                    if (prefix != null && name.length() > prefix.length()) {
                        name = descriptor.getName(tag).substring(prefix.length());
                    }
                    LookupElementBuilder element = LookupElementBuilder.create(name);
                    if (descriptor instanceof PsiPresentableMetaData presentableMetaData) {
                        element = element.withIcon(presentableMetaData.getIcon());
                    }
                    int separator = name.indexOf(':');
                    if (separator > 0) {
                        element = element.withLookupString(name.substring(separator + 1));
                    }
                    element = element.withCaseSensitivity(!(descriptor instanceof HtmlAttributeDescriptorImpl))
                        .withInsertHandler(insertHandler);
                    result.addElement(
                        descriptor.isRequired()
                            ? PrioritizedLookupElement.withPriority(element.appendTailText("(required)", true), 100)
                            : HtmlUtil.isOwnHtmlAttribute(descriptor)
                            ? PrioritizedLookupElement.withPriority(element, 50)
                            : element
                    );
                }
            }
        }
    }

    private static boolean isValidVariant(
        XmlAttribute attribute,
        @Nonnull XmlAttributeDescriptor descriptor,
        XmlAttribute[] attributes,
        XmlExtension extension
    ) {
        if (extension.isIndirectSyntax(descriptor)) {
            return false;
        }
        String descriptorName = descriptor.getName(attribute.getParent());
        if (descriptorName == null) {
            LOG.error("Null descriptor name for " + descriptor + " " + descriptor.getClass() + " ");
            return false;
        }
        for (XmlAttribute otherAttr : attributes) {
            if (otherAttr != attribute && otherAttr.getName().equals(descriptorName)) {
                return false;
            }
        }
        return !descriptorName.contains(DUMMY_IDENTIFIER_TRIMMED);
    }

}
