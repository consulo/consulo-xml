package com.intellij.xml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.xml.psi.xml.XmlTag;

/**
 * @author Eugene.Kudelevsky
 */
public interface NamespaceAwareXmlAttributeDescriptor extends XmlAttributeDescriptor {
    @Nullable
    String getNamespace(@Nonnull XmlTag context);
}
