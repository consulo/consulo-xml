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

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.util.xml.AbstractDomDeclarationSearcher;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomTarget;
import consulo.xml.util.xml.NameValue;

import jakarta.annotation.Nullable;

/**
 * @author peter
 */
@ExtensionImpl
public class DomDeclarationSearcher extends AbstractDomDeclarationSearcher {

  @Nullable
  protected DomTarget createDomTarget(DomElement parent, DomElement nameElement) {
    final NameValue nameValue = nameElement.getAnnotation(NameValue.class);
    if (nameValue != null && nameValue.referencable()) {
      return DomTarget.getTarget(parent);
    }
    return null;
  }

}
