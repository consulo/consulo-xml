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
package com.intellij.xml.impl.schema;

import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.util.XmlUtil;
import consulo.language.psi.PsiElement;
import consulo.xml.Validator;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.HashMap;

/**
 * @author Maxim.Mossienko
 */
public class SchemaNSDescriptor extends XmlNSDescriptorImpl {
    @NonNls
    private static final String MIN_OCCURS_ATTR_NAME = "minOccurs";
    @NonNls
    private static final String MAX_OCCURS_ATTR_VALUE = "maxOccurs";
    @NonNls
    private static final String MAX_OCCURS_ATTR_NAME = MAX_OCCURS_ATTR_VALUE;
    @NonNls
    private static final String ID_ATTR_NAME = "id";
    @NonNls
    private static final String REF_ATTR_NAME = "ref";
    @NonNls
    private static final String DEFAULT_ATTR_NAME = "default";
    @NonNls
    private static final String FIXED_ATTR_NAME = "fixed";

    @NonNls
    private static final String NAME_ATTR_NAME = "name";

    private static final Validator<XmlTag> ELEMENT_VALIDATOR = new Validator<XmlTag>() {
        public void validate(@Nonnull final XmlTag tag, @Nonnull Validator.ValidationHost host) {
            if (!isFromSchemaNs(tag)) {
                return;
            }
            final boolean hasRefAttribute = tag.getAttributeValue(REF_ATTR_NAME) != null;

            if (hasRefAttribute) {
                for (XmlAttribute attr : tag.getAttributes()) {
                    final String name = attr.getName();

                    if (name.indexOf(':') == -1 &&
                        !MIN_OCCURS_ATTR_NAME.equals(name) &&
                        !MAX_OCCURS_ATTR_NAME.equals(name) &&
                        !ID_ATTR_NAME.equals(name) &&
                        !REF_ATTR_NAME.equals(name)) {
                        host.addMessage(
                            attr.getNameElement(),
                            XmlErrorLocalize.xmlSchemaValidationAttrNotAllowedWithRef(name).get(),
                            ValidationHost.ErrorType.ERROR
                        );
                    }
                }
            }

            final String minOccursValue = tag.getAttributeValue("minOccurs");
            final String maxOccursValue = tag.getAttributeValue(MAX_OCCURS_ATTR_VALUE);

            if (minOccursValue != null && maxOccursValue != null) {
                try {
                    final int minOccurs = Integer.parseInt(minOccursValue);
                    final int maxOccurs = Integer.parseInt(maxOccursValue);
                    if (maxOccurs < minOccurs) {
                        host.addMessage(
                            tag.getAttribute(MAX_OCCURS_ATTR_VALUE, null).getValueElement(),
                            XmlErrorLocalize.xmlSchemaValidationMaxOccursShouldBeNotLessThanMinOccurs().get(),
                            ValidationHost.ErrorType.ERROR
                        );
                    }
                }
                catch (NumberFormatException e) {
                    // this schema will be reported by xerces validation
                }
            }

            if (!hasRefAttribute && tag.getAttributeValue(NAME_ATTR_NAME) == null) {
                host.addMessage(
                    tag,
                    XmlErrorLocalize.xmlSchemaValidationNameOrRefShouldPresent().get(),
                    ValidationHost.ErrorType.ERROR
                );
            }
        }
    };

    private static final Validator<XmlTag> ATTRIBUTE_VALIDATOR = new Validator<XmlTag>() {
        public void validate(@Nonnull final XmlTag tag, @Nonnull ValidationHost host) {
            if (!isFromSchemaNs(tag)) {
                return;
            }

            if (tag.getAttributeValue(REF_ATTR_NAME) == null && tag.getAttributeValue(NAME_ATTR_NAME) == null) {
                host.addMessage(
                    tag,
                    XmlErrorLocalize.xmlSchemaValidationNameOrRefShouldPresent().get(),
                    ValidationHost.ErrorType.ERROR
                );
            }

            if (tag.getAttributeValue(DEFAULT_ATTR_NAME) != null && tag.getAttributeValue(FIXED_ATTR_NAME) != null) {
                host.addMessage(
                    tag.getAttribute(DEFAULT_ATTR_NAME, null).getNameElement(),
                    XmlErrorLocalize.xmlSchemaValidationDefaultOrFixedShouldBeSpecifiedButNotBoth().get(),
                    ValidationHost.ErrorType.ERROR
                );

                host.addMessage(
                    tag.getAttribute(FIXED_ATTR_NAME, null).getNameElement(),
                    XmlErrorLocalize.xmlSchemaValidationDefaultOrFixedShouldBeSpecifiedButNotBoth().get(),
                    ValidationHost.ErrorType.ERROR
                );
            }
        }
    };

    private static final XmlUtil.DuplicationInfoProvider<XmlTag> SCHEMA_ATTR_DUP_INFO_PROVIDER =
        new XmlUtil.DuplicationInfoProvider<XmlTag>() {
            public String getName(@Nonnull final XmlTag t) {
                return t.getAttributeValue(NAME_ATTR_NAME);
            }

            @Nonnull
            public String getNameKey(@Nonnull final XmlTag t, @Nonnull String name) {
                return name;
            }

            @Nonnull
            public PsiElement getNodeForMessage(@Nonnull final XmlTag t) {
                return t.getAttribute(NAME_ATTR_NAME, null).getValueElement();
            }
        };

    private static final Validator<XmlTag> ELEMENT_AND_ATTR_VALIDATOR = new Validator<XmlTag>() {
        public void validate(@Nonnull final XmlTag tag, @Nonnull ValidationHost host) {
            if (!isFromSchemaNs(tag)) {
                return;
            }
            final String nsPrefix = tag.getNamespacePrefix();
            final XmlTag[] attrDeclTags = tag.findSubTags((nsPrefix.length() > 0 ? nsPrefix + ":" : "") + "attribute");

            XmlUtil.doDuplicationCheckForElements(
                attrDeclTags,
                new HashMap<>(attrDeclTags.length),
                SCHEMA_ATTR_DUP_INFO_PROVIDER,
                host
            );

            final XmlTag[] elementDeclTags = tag.findSubTags((nsPrefix.length() > 0 ? nsPrefix + ":" : "") + "element");

            XmlUtil.doDuplicationCheckForElements(
                elementDeclTags,
                new HashMap<>(elementDeclTags.length),
                SCHEMA_ATTR_DUP_INFO_PROVIDER,
                host
            );
        }
    };

    private static boolean isFromSchemaNs(final XmlTag tag) {
        return XmlUtil.XML_SCHEMA_URI.equals(tag.getNamespace());
    }

    protected XmlElementDescriptor createElementDescriptor(final XmlTag tag) {
        final XmlElementDescriptor descriptor = super.createElementDescriptor(tag);
        String localName = tag.getAttributeValue(NAME_ATTR_NAME);
        if (ELEMENT_TAG_NAME.equals(localName)) {
            ((XmlElementDescriptorImpl)descriptor).setValidator(ELEMENT_VALIDATOR);
        }
        else if (COMPLEX_TYPE_TAG_NAME.equals(localName) || SCHEMA_TAG_NAME.equals(localName) || SEQUENCE_TAG_NAME.equals(localName)) {
            ((XmlElementDescriptorImpl)descriptor).setValidator(ELEMENT_AND_ATTR_VALIDATOR);
        }
        else if (ATTRIBUTE_TAG_NAME.equals(localName)) {
            ((XmlElementDescriptorImpl)descriptor).setValidator(ATTRIBUTE_VALIDATOR);
        }
        return descriptor;
    }

    @Override
    public String toString() {
        return getDefaultNamespace();
    }
}
