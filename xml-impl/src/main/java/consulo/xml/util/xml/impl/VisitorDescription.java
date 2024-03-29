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
package consulo.xml.util.xml.impl;

import java.lang.reflect.Method;

import org.jetbrains.annotations.NonNls;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.ide.impl.idea.util.containers.ConcurrentClassMap;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomElementVisitor;
import consulo.xml.util.xml.DomReflectionUtil;

/**
 * @author peter
 */
public class VisitorDescription {
  private final Class<? extends DomElementVisitor> myVisitorClass;
  private final ConcurrentClassMap<Method> myMethods = new ConcurrentClassMap<Method>();
  @NonNls private static final String VISIT = "visit";

  public VisitorDescription(final Class<? extends DomElementVisitor> visitorClass) {
    myVisitorClass = visitorClass;
    for (final Method method : ReflectionUtil.getClassPublicMethods(visitorClass)) {
      final Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length != 1) {
        continue;
      }
      final Class<?> domClass = parameterTypes[0];
      if (!ReflectionUtil.isAssignable(DomElement.class, domClass)) {
        continue;
      }
      final String methodName = method.getName();
      if (VISIT.equals(methodName) || methodName.startsWith(VISIT) /*&& domClass.getSimpleName().equals(methodName.substring(VISIT.length()))*/) {
        method.setAccessible(true);
        myMethods.put(domClass, method);
      }
    }
  }

  public void acceptElement(DomElementVisitor visitor, DomElement element) {
    final Method method = myMethods.get(element.getClass());
    assert method != null : myVisitorClass + " can't accept element of type " + element.getClass();
    DomReflectionUtil.invokeMethod(method, visitor, element);
  }

}
