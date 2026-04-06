// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.html;

import com.intellij.html.impl.RelaxedHtmlFromSchemaElementDescriptor;
import consulo.xml.descriptor.XmlAttributeDescriptor;
import consulo.xml.descriptor.XmlElementDescriptor;
import consulo.xml.descriptor.XmlNSDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlTag;

public class RelaxedAnyHtmlElementDescriptor extends AnyXmlElementDescriptor {

    public RelaxedAnyHtmlElementDescriptor(XmlElementDescriptor parentDescriptor,
                                           XmlNSDescriptor xmlNSDescriptor) {
        super(parentDescriptor, xmlNSDescriptor);
    }

    @Override
    public XmlAttributeDescriptor[] getAttributesDescriptors(XmlTag context) {
        return RelaxedHtmlFromSchemaElementDescriptor.addAttrDescriptorsForFacelets(context, XmlAttributeDescriptor.EMPTY);
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(String attributeName, XmlTag context) {
        XmlAttributeDescriptor descriptor = RelaxedHtmlFromSchemaElementDescriptor.getAttributeDescriptorFromFacelets(attributeName, context);
        if (descriptor == null) {
            descriptor = super.getAttributeDescriptor(attributeName, context);
        }
        return descriptor;
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attr) {
        XmlAttributeDescriptor descriptor = RelaxedHtmlFromSchemaElementDescriptor.getAttributeDescriptorFromFacelets(attr.getName(), attr.getParent());
        if (descriptor == null) {
            descriptor = super.getAttributeDescriptor(attr);
        }
        return descriptor;
    }
}
