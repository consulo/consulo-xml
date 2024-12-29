package consulo.xml.util.xml.impl;

import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public interface Invocation {
  @Nullable
  Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable;

}
