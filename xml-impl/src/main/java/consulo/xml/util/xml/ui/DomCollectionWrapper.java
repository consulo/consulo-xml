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
package consulo.xml.util.xml.ui;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.util.lang.reflect.ReflectionUtil;

import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author peter
 */
public class DomCollectionWrapper<T> extends DomWrapper<T>{
  private final DomElement myDomElement;
  private final DomCollectionChildDescription myChildDescription;
  private final Method mySetter;
  private final Method myGetter;

  public DomCollectionWrapper(final DomElement domElement,
                              final DomCollectionChildDescription childDescription) {
    this(domElement, childDescription, 
         DomUIFactory.findMethod(ReflectionUtil.getRawType(childDescription.getType()), "setValue"),
         DomUIFactory.findMethod(ReflectionUtil.getRawType(childDescription.getType()), "getValue"));
  }

  public DomCollectionWrapper(final DomElement domElement,
                              final DomCollectionChildDescription childDescription,
                              final Method setter,
                              final Method getter) {
    myDomElement = domElement;
    myChildDescription = childDescription;
    mySetter = setter;
    myGetter = getter;
  }

  @Nonnull
  public DomElement getExistingDomElement() {
    return myDomElement;
  }

  public DomElement getWrappedElement() {
    final List<? extends DomElement> list = myChildDescription.getValues(myDomElement);
    return list.isEmpty() ? null : list.get(0);
  }

  public void setValue(final T value) throws IllegalAccessException, InvocationTargetException {
    final List<? extends DomElement> list = myChildDescription.getValues(myDomElement);
    final DomElement domElement;
    if (list.isEmpty()) {
      domElement = myChildDescription.addValue(myDomElement);
    } else {
      domElement = list.get(0);
    }
    mySetter.invoke(domElement, value);
  }

  public T getValue() throws IllegalAccessException, InvocationTargetException {
    if (!myDomElement.isValid()) return null;
    final List<? extends DomElement> list = myChildDescription.getValues(myDomElement);
    return list.isEmpty() ? null : (T)myGetter.invoke(list.get(0));
  }

}
