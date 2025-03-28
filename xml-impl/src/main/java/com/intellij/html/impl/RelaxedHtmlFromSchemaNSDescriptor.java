/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.html.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import com.intellij.html.RelaxedHtmlNSDescriptor;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlTag;
import consulo.util.collection.ArrayUtil;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.impl.schema.XmlNSDescriptorImpl;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlUtil;

public class RelaxedHtmlFromSchemaNSDescriptor extends XmlNSDescriptorImpl implements RelaxedHtmlNSDescriptor {
    @Override
    public XmlElementDescriptor getElementDescriptor(@Nonnull XmlTag tag) {
        XmlElementDescriptor elementDescriptor = super.getElementDescriptor(tag);

        String namespace;
        if (elementDescriptor == null && !XmlUtil.XHTML_URI.equals(namespace = tag.getNamespace())) {
            return new AnyXmlElementDescriptor(
                null,
                XmlUtil.HTML_URI.equals(namespace) ? this : tag.getNSDescriptor(tag.getNamespace(), true)
            );
        }

        return elementDescriptor;
    }

    @Override
    protected XmlElementDescriptor createElementDescriptor(final XmlTag tag) {
        return new RelaxedHtmlFromSchemaElementDescriptor(tag);
    }

    @Override
    @Nonnull
    public XmlElementDescriptor[] getRootElementsDescriptors(@Nullable final XmlDocument doc) {
        return ArrayUtil.mergeArrays(super.getRootElementsDescriptors(doc), HtmlUtil.getCustomTagDescriptors(doc));
    }
}
