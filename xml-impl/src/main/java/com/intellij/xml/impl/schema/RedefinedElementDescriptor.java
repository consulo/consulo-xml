package com.intellij.xml.impl.schema;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;

import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
public class RedefinedElementDescriptor extends XmlElementDescriptorImpl {
    private final XmlNSDescriptorImpl myRedefinedNSDescriptor;

    public RedefinedElementDescriptor(XmlElementDescriptorImpl original, XmlNSDescriptorImpl xmlNSDescriptor) {
        super(original.myDescriptorTag);
        myRedefinedNSDescriptor = xmlNSDescriptor;
    }

    @Override
    public TypeDescriptor getType(XmlElement context) {
        TypeDescriptor typeDescriptor = super.getType(context);
        return typeDescriptor instanceof ComplexTypeDescriptor complexTypeDescriptor
            ? new RedefinedTypeDescriptor(complexTypeDescriptor) : typeDescriptor;
    }

    private class RedefinedTypeDescriptor extends ComplexTypeDescriptor {
        public RedefinedTypeDescriptor(ComplexTypeDescriptor original) {
            super(original.getNsDescriptor(), original.getDeclaration());
        }

        @Override
        protected XmlSchemaTagsProcessor createProcessor(final Map<String, XmlElementDescriptor> map) {
            return new XmlSchemaTagsProcessor(myDocumentDescriptor) {
                @Override
                protected void tagStarted(XmlTag tag, String tagName, XmlTag context, XmlTag ref) {
                    addElementDescriptor(tag, tagName, map);
                    if ("extension".equals(tagName)) {
                        String base = tag.getAttributeValue("base");
                        if (base != null) {
                            TypeDescriptor descriptor = myRedefinedNSDescriptor.findTypeDescriptor(base);
                            if (descriptor instanceof ComplexTypeDescriptor complexTypeDescriptor) {
                                XmlElementDescriptor[] elements = complexTypeDescriptor.getElements(null);
                                for (XmlElementDescriptor element : elements) {
                                    addElementDescriptor(map, element);
                                }
                            }
                        }
                    }
                }
            };
        }
    }
}