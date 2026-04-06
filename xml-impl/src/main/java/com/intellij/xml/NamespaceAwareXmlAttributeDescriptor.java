package com.intellij.xml;

import consulo.xml.descriptor.XmlAttributeDescriptor;
import consulo.xml.language.psi.XmlTag;
import org.jspecify.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public interface NamespaceAwareXmlAttributeDescriptor extends XmlAttributeDescriptor {
    @Nullable
    String getNamespace(XmlTag context);
}
