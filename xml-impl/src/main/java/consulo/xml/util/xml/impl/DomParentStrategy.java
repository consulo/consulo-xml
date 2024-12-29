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

import jakarta.annotation.Nonnull;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;

import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public interface DomParentStrategy {
  @Nullable DomInvocationHandler getParentHandler();

  @Nullable
  XmlElement getXmlElement();

  @Nonnull
  DomParentStrategy refreshStrategy(final DomInvocationHandler handler);

  @Nonnull
  DomParentStrategy setXmlElement(@Nonnull XmlElement element);

  @Nonnull
  DomParentStrategy clearXmlElement();

  @Nullable
  String checkValidity();

  XmlFile getContainingFile(DomInvocationHandler handler);

  boolean isPhysical();
}
