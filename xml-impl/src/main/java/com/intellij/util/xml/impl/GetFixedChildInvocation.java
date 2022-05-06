package com.intellij.util.xml.impl;

import consulo.util.lang.Pair;

/**
 * @author peter
 */
public class GetFixedChildInvocation implements Invocation {
  private final Pair<FixedChildDescriptionImpl,Integer> myPair;

  public GetFixedChildInvocation(final Pair<FixedChildDescriptionImpl, Integer> pair) {
    myPair = pair;
  }

  public Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable {
    return handler.getFixedChild(myPair).getProxy();
  }
}
