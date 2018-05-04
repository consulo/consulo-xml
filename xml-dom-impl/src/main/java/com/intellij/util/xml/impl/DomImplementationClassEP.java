package com.intellij.util.xml.impl;

import javax.annotation.Nullable;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * @author peter
 */
public class DomImplementationClassEP extends AbstractExtensionPointBean {
  public static final ExtensionPointName<DomImplementationClassEP> EP_NAME = ExtensionPointName.create("com.intellij.xml.dom.implementation");
  public static final ExtensionPointName<DomImplementationClassEP> CONVERTER_EP_NAME = ExtensionPointName.create("com.intellij.xml.dom.converter");

  @Attribute("interfaceClass")
  public String interfaceName;

  @Attribute("implementationClass")
  public String implementationName;


  @Nullable
  public Class getInterfaceClass() {
    return findClassNoExceptions(interfaceName);
  }

  @Nullable
  public Class getImplementationClass() {
    return findClassNoExceptions(implementationName);
  }
}
