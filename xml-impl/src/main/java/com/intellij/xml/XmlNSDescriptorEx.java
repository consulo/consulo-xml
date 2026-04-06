package com.intellij.xml;

import consulo.xml.descriptor.XmlElementDescriptor;
import consulo.xml.descriptor.XmlNSDescriptor;

/**
 * @author Eugene.Kudelevsky
 */
public interface XmlNSDescriptorEx extends XmlNSDescriptor {
    XmlElementDescriptor getElementDescriptor(String localName, String namespace);
}
