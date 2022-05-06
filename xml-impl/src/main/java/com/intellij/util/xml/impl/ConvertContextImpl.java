package com.intellij.util.xml.impl;

import com.intellij.psi.PsiManager;
import com.intellij.util.xml.AbstractConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.openapi.module.Module;
import javax.annotation.Nonnull;

/**
 * @author peter
 */
public class ConvertContextImpl extends AbstractConvertContext {
  private final DomInvocationHandler myHandler;

  public ConvertContextImpl(final DomInvocationHandler handler) {
    myHandler = handler;
  }

  public ConvertContextImpl(DomElement element) {
    this(DomManagerImpl.getDomInvocationHandler(element));
  }

  @Nonnull
  public DomElement getInvocationElement() {
    return myHandler.getProxy();
  }

  public PsiManager getPsiManager() {
    return myHandler.getFile().getManager();
  }

  public Module getModule() {
    final DomElement domElement = getInvocationElement();
    if (domElement.getManager().isMockElement(domElement)) {
      return getInvocationElement().getModule();
    }
    return super.getModule();
  }
}
