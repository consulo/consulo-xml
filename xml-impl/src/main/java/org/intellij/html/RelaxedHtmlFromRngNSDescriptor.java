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
package org.intellij.html;

import com.intellij.html.RelaxedHtmlNSDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.plugins.relaxNG.model.descriptors.RngNsDescriptor;

import java.util.Arrays;

/**
 * @author Eugene.Kudelevsky
 */
public class RelaxedHtmlFromRngNSDescriptor extends RngNsDescriptor implements RelaxedHtmlNSDescriptor {
    private static final Logger LOG = Logger.getInstance("#RelaxedHtmlFromRngNSDescriptor");

    @Override
    public XmlElementDescriptor getElementDescriptor(@Nonnull XmlTag tag) {
        XmlElementDescriptor elementDescriptor = super.getElementDescriptor(tag);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Descriptor from rng for tag " +
                tag.getName() +
                " is " +
                (elementDescriptor != null ? elementDescriptor.getClass().getCanonicalName() : "NULL"));
        }

        String namespace;
        if (elementDescriptor == null &&
            !((namespace = tag.getNamespace()).equals(XmlUtil.XHTML_URI))) {
            var nsDescriptor = XmlUtil.HTML_URI.equals(namespace) ? this : tag.getNSDescriptor(namespace, true);
            if (XmlUtil.HTML_URI.equals(namespace) || HtmlUtil.MATH_ML_NAMESPACE.equals(namespace) || HtmlUtil.SVG_NAMESPACE.equals(namespace)) {
                return new RelaxedAnyHtmlElementDescriptor(null, nsDescriptor);
            }
            else {
                return new AnyXmlElementDescriptor(null, nsDescriptor);
            }
        }

        return elementDescriptor;
    }

    @Override
    protected XmlElementDescriptor initDescriptor(@Nonnull XmlElementDescriptor descriptor) {
        return new RelaxedHtmlFromRngElementDescriptor(descriptor);
    }

    @Override
    @Nonnull
    public XmlElementDescriptor[] getRootElementsDescriptors(@Nullable final XmlDocument doc) {
        final XmlElementDescriptor[] descriptors = super.getRootElementsDescriptors(doc);
        /**
         * HTML 5 descriptor list contains not only HTML elements, but also SVG and MathML. To prevent conflicts
         * we need to prioritize HTML ones {@link RelaxedHtmlFromRngElementDescriptor#compareTo(Object)}
         */
        Arrays.sort(descriptors);
        return ArrayUtil.mergeArrays(descriptors, HtmlUtil.getCustomTagDescriptors(doc));
    }
}
