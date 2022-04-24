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

import consulo.language.psi.util.CachedValue;
import consulo.language.psi.util.CachedValueProvider;
import consulo.language.psi.util.CachedValuesManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class DomModelCache<T, H extends UserDataHolder> {

  private final Key<CachedValue<T>> myKey;
  private final Project myProject;

  public DomModelCache(Project project, @NonNls String keyName) {
    myProject = project;
    myKey = Key.create(keyName);
  }

  @Nullable
  public T getCachedValue(final @Nonnull H dataHolder) {
    CachedValue<T> cachedValue = dataHolder.getUserData(myKey);
    if (cachedValue == null) {
      final CachedValueProvider<T> myProvider = new CachedValueProvider<T>() {
        @Nullable
        public Result<T> compute() {
          return computeValue(dataHolder);
        }
      };
      final CachedValuesManager manager = CachedValuesManager.getManager(myProject);
      cachedValue = manager.createCachedValue(myProvider, false);
      dataHolder.putUserData(myKey, cachedValue);
    }
    return cachedValue.getValue();
  }

  @Nonnull
  protected abstract CachedValueProvider.Result<T> computeValue(@Nonnull H dataHolder);
}
