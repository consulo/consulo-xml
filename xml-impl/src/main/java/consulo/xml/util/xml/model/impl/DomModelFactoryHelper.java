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
package consulo.xml.util.xml.model.impl;

import consulo.xml.util.xml.ModelMerger;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.DomElement;
import consulo.xml.psi.xml.XmlFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * User: Sergey.Vasiliev
 */
public class DomModelFactoryHelper<T extends DomElement> {
  protected final Class<T> myClass;
  protected final ModelMerger myModelMerger;

  public DomModelFactoryHelper(@Nonnull Class<T> aClass, @Nonnull ModelMerger modelMerger) {
    myClass = aClass;
    myModelMerger = modelMerger;
  }

  @Nullable
  public T getDom(@Nonnull XmlFile configFile) {
    final DomFileElement<T> element = getDomRoot(configFile);
    return element == null ? null : element.getRootElement();
  }

  @Nullable
  public DomFileElement<T> getDomRoot(@Nonnull XmlFile configFile) {
    return DomManager.getDomManager(configFile.getProject()).getFileElement(configFile, myClass);
  }

  public Class<T> getDomModelClass() {
    return myClass;
  }

  public ModelMerger getModelMerger() {
    return myModelMerger;
  }
}
