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
import consulo.xml.util.xml.GenericDomValue;
import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;

/**
 * @author peter
 */
public class DomStringWrapper extends DomWrapper<String>{
  private final GenericDomValue myDomElement;

  public DomStringWrapper(final GenericDomValue domElement) {
    myDomElement = domElement;
  }

  @Nonnull
  public DomElement getExistingDomElement() {
    return myDomElement;
  }

  public DomElement getWrappedElement() {
    return myDomElement;
  }

  public void setValue(final String value) throws IllegalAccessException, InvocationTargetException {
    myDomElement.setStringValue(value);
  }

  public String getValue() throws IllegalAccessException, InvocationTargetException {
    return myDomElement.isValid() ? myDomElement.getStringValue() : null;
  }

}
