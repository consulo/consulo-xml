/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.xml.impl;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Factory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.MergedObject;
import com.intellij.util.xml.StableElement;
import consulo.util.advandedProxy.ObjectMethods;
import consulo.xml.dom.util.proxy.InvocationHandlerOwner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
class StableInvocationHandler<T> implements InvocationHandler, StableElement {
  private T myOldValue;
  private T myCachedValue;
  private final Set<Class> myClasses;
  private final Factory<T> myProvider;
  private final Condition<T> myValidator;

  public StableInvocationHandler(final T initial, final Factory<T> provider, Condition<T> validator) {
    myProvider = provider;
    myCachedValue = initial;
    myOldValue = initial;
    myValidator = validator;
    final Class superClass = initial.getClass().getSuperclass();
    final Set<Class> classes = new HashSet<Class>();
    ContainerUtil.addAll(classes, initial.getClass().getInterfaces());
    ContainerUtil.addIfNotNull(classes, superClass);
    classes.remove(MergedObject.class);
    myClasses = classes;
  }


  public final Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
    if (StableElement.class.equals(method.getDeclaringClass())) {
      try {
        return method.invoke(this, args);
      }
      catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }

    if(InvocationHandlerOwner.METHOD.equals(method)) return this;

    if (ObjectMethods.FINALIZE_METHOD.equals(method)) return null;

    if (isNotValid(myCachedValue)) {
      if (myCachedValue != null) {
        myOldValue = myCachedValue;
      }
      myCachedValue = myProvider.create();
      if (isNotValid(myCachedValue)) {
        if (ObjectMethods.EQUALS_METHOD.equals(method)) {

          final Object arg = args[0];
          if (!(arg instanceof StableElement)) return false;

          final StableInvocationHandler handler = DomManagerImpl.getStableInvocationHandler(arg);
          if (handler.getWrappedElement() != null) return false;

          return Comparing.equal(myOldValue, handler.myOldValue);
        }

        if (myOldValue != null && Object.class.equals(method.getDeclaringClass())) {
          return method.invoke(myOldValue, args);
        }

        if ("isValid".equals(method.getName())) {
          return Boolean.FALSE;
        }
        throw new AssertionError("Calling methods on invalid value");
      }
    }

    if (ObjectMethods.EQUALS_METHOD.equals(method)) {
      final Object arg = args[0];
      if (arg instanceof StableElement) {
        return myCachedValue.equals(((StableElement)arg).getWrappedElement());
      }
      return myCachedValue.equals(arg);

    }
    if (ObjectMethods.HASHCODE_METHOD.equals(method)) {
      return myCachedValue.hashCode();
    }

    try {
      return method.invoke(myCachedValue, args);
    }
    catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  public final void revalidate() {
    final T t = myProvider.create();
    if (!isNotValid(t) && !t.equals(myCachedValue)) {
      myCachedValue = t;
    }
  }

  public final void invalidate() {
    if (!isNotValid(myCachedValue)) {
      myCachedValue = null;
    }
  }

  public final T getWrappedElement() {
    if (isNotValid(myCachedValue)) {
      myCachedValue = myProvider.create();
    }
    return myCachedValue;
  }

  public T getOldValue() {
    return myOldValue;
  }

  private boolean isNotValid(final T t) {
    if (t == null || !myValidator.value(t)) return true;
    for (final Class aClass : myClasses) {
      if (!aClass.isInstance(t)) return true;
    }
    return false;
  }
}
