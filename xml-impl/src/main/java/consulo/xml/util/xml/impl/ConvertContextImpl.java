package consulo.xml.util.xml.impl;

import consulo.language.psi.PsiManager;
import consulo.xml.dom.AbstractConvertContext;
import consulo.xml.dom.DomElement;
import consulo.module.Module;

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
