package com.intellij.xml;

import org.jspecify.annotations.Nullable;

import consulo.xml.psi.xml.XmlTag;

/**
 * @author Eugene.Kudelevsky
 */
public interface NamespaceAwareXmlAttributeDescriptor extends XmlAttributeDescriptor {
    @Nullable
    String getNamespace(XmlTag context);
}
