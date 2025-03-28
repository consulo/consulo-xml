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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.xml.psi.xml.XmlFile;
import consulo.util.collection.ContainerUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.ModelMerger;
import consulo.xml.util.xml.model.DomModel;
import consulo.xml.util.xml.model.SimpleModelFactory;

/**
 * @author Dmitry Avdeev
 */
public abstract class SimpleDomModelFactory<T extends DomElement, M extends DomModel<T>> extends DomModelFactoryHelper<T> implements
                                                                                                                         SimpleModelFactory<T, M> {

  public SimpleDomModelFactory(@Nonnull Class<T> aClass, @Nonnull ModelMerger modelMerger) {
    super(aClass, modelMerger);
  }

  @Nullable
  public DomFileElement<T> createMergedModelRoot(Set<XmlFile> configFiles) {
    List<DomFileElement<T>> configs = new ArrayList<DomFileElement<T>>(configFiles.size());
    for (XmlFile configFile : configFiles) {
      ContainerUtil.addIfNotNull(configs, getDomRoot(configFile));
    }
    return configs.isEmpty() ? null : getModelMerger().mergeModels(DomFileElement.class, configs);
  }
}
