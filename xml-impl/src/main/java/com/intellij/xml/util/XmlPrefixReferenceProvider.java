package com.intellij.xml.util;

import consulo.language.psi.PsiReference;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.language.util.ProcessingContext;
import com.intellij.xml.impl.schema.XmlAttributeDescriptorImpl;
import com.intellij.xml.impl.schema.XmlNSDescriptorImpl;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceProvider;


/**
 * @author Dmitry Avdeev
 */
public class XmlPrefixReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
        XmlAttributeValue attributeValue = (XmlAttributeValue)element;
        PsiElement parent = attributeValue.getParent();
        if (parent instanceof XmlAttribute attribute
            && !XmlNSDescriptorImpl.checkSchemaNamespace(attribute.getParent())
            && attribute.getDescriptor() instanceof XmlAttributeDescriptorImpl attributeDescriptor) {
            String type = attributeDescriptor.getType();
            if (type != null && type.endsWith(":QName")) {
                String prefix = XmlUtil.findPrefixByQualifiedName(type);
                String ns = ((XmlTag)attributeDescriptor.getDeclaration()).getNamespaceByPrefix(prefix);
                if (XmlNSDescriptorImpl.checkSchemaNamespace(ns)) {
                    String value = attributeValue.getValue();
                    if (value != null) {
                        int i = value.indexOf(':');
                        if (i > 0) {
                            return new PsiReference[]{new SchemaPrefixReference(
                                attributeValue,
                                TextRange.from(1, i),
                                value.substring(0, i),
                                null
                            )};
                        }
                    }
                }
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
