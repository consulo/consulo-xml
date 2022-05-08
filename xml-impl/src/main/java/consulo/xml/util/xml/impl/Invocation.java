package consulo.xml.util.xml.impl;

import javax.annotation.Nullable;

/**
 * @author peter
 */
public interface Invocation {
  @Nullable
  Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable;

}
