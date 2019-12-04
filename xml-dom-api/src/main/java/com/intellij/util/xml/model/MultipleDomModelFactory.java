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
package com.intellij.util.xml.model;

import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * User: Sergey.Vasiliev
 */
public interface MultipleDomModelFactory<Scope extends UserDataHolder, T extends DomElement, M extends DomModel<T>> {
  @Nonnull
  List<M> getAllModels(@Nonnull Scope scope);

  Set<XmlFile> getAllConfigFiles(@Nonnull Scope scope);

  @Nullable
  M getCombinedModel(@Nullable Scope scope);
}
