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
package com.intellij.util.xml;

import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import consulo.component.util.ModificationTracker;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public interface DomFileElement<T extends DomElement> extends DomElement, UserDataHolder, ModificationTracker {
  @Nonnull
  XmlFile getFile();

  @Nonnull
  XmlFile getOriginalFile();

  @Nullable
  XmlTag getRootTag();

  @Nonnull
  T getRootElement();

  @Nonnull
  Class<T> getRootElementClass();

  @Nonnull
  DomFileDescription<T> getFileDescription();

}
