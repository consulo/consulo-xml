package com.intellij.xml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.psi.xml.XmlTag;

/**
 * @author Eugene.Kudelevsky
 */
public interface NamespaceAwareXmlAttributeDescriptor extends XmlAttributeDescriptor {
  @Nullable
  String getNamespace(@Nonnull XmlTag context);
}
