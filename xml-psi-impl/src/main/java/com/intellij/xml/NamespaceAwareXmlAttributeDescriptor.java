package com.intellij.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.psi.xml.XmlTag;

/**
 * @author Eugene.Kudelevsky
 */
public interface NamespaceAwareXmlAttributeDescriptor extends XmlAttributeDescriptor {
  @Nullable
  String getNamespace(@NotNull XmlTag context);
}
