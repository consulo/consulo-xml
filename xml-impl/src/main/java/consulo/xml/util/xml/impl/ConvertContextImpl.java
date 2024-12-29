package consulo.xml.util.xml.impl;

import consulo.language.psi.PsiManager;
import consulo.xml.util.xml.AbstractConvertContext;
import consulo.xml.util.xml.DomElement;
import consulo.module.Module;
import jakarta.annotation.Nonnull;

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
