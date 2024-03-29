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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import consulo.application.util.CachedValueProvider;
import consulo.application.util.UserDataCache;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiFile;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValuesManager;
import consulo.util.dataholder.Key;

/**
 * @author Maxim.Mossienko
*         Date: 30.12.2008
*         Time: 21:03:42
*/
public abstract class FileBasedUserDataCache<T> extends UserDataCache<CachedValue<T>, PsiFile, Object> {
  protected CachedValue<T> compute(final PsiFile xmlFile, final Object o) {
    return CachedValuesManager.getManager(xmlFile.getProject()).createCachedValue(new CachedValueProvider<T>() {
      public Result<T> compute() {

        return new Result<T>(doCompute(xmlFile), getDependencies(xmlFile));
      }
    }, false);
  }

  protected Object[] getDependencies(PsiFile xmlFile) {
    return new Object[] {xmlFile};
  }

  protected abstract T doCompute(PsiFile file);
  protected abstract Key<CachedValue<T>> getKey();

  public T compute(PsiFile file) {
    final FileViewProvider fileViewProvider = file.getViewProvider();
    final PsiFile baseFile = fileViewProvider.getPsi(fileViewProvider.getBaseLanguage());
    baseFile.getFirstChild(); // expand chameleon out of lock
    return get(getKey(), baseFile, null).getValue();
  }
}
