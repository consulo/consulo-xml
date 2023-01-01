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
package consulo.xml.util.xml.reflect;

import javax.annotation.Nonnull;

import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomNameStrategy;
import consulo.xml.util.xml.XmlName;
import consulo.language.pom.PomNamedTarget;

/**
 * @author peter
 */
public interface DomChildrenDescription extends AbstractDomChildrenDescription, PomNamedTarget {

  @Nonnull
  XmlName getXmlName();

  @Nonnull
  String getXmlElementName();

  @Nonnull
  String getCommonPresentableName(@Nonnull DomNameStrategy strategy);

  @Nonnull
  String getCommonPresentableName(@Nonnull DomElement parent);

}
