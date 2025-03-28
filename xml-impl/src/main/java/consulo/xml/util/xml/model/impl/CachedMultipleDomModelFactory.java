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

import consulo.xml.psi.xml.XmlFile;
import consulo.util.collection.ContainerUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.ModelMerger;
import consulo.xml.util.xml.model.DomModel;
import consulo.xml.util.xml.model.DomModelCache;
import consulo.xml.util.xml.model.MultipleDomModelFactory;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * User: Sergey.Vasiliev
 */
public abstract class CachedMultipleDomModelFactory<Scope extends UserDataHolder, T extends DomElement, M extends DomModel<T>, C extends PsiElement>
    extends DomModelFactoryHelper<T>
    implements CachedDomModelFactory<T,M,Scope>, MultipleDomModelFactory<Scope,T,M> {
  
    private final DomModelCache<M, Scope> myCombinedModelCache;
    private final DomModelCache<List<M>, Scope> myAllModelsCache;


    protected CachedMultipleDomModelFactory(@Nonnull Class<T> aClass,
                            @Nonnull ModelMerger modelMerger,
                            final Project project,
                            @NonNls String name) {
      super(aClass,modelMerger);

      myCombinedModelCache = new DomModelCache<M, Scope>(project, name + " combined model") {
        @Nonnull
        protected CachedValueProvider.Result<M> computeValue(@Nonnull final Scope scope) {
          final M combinedModel = computeCombinedModel(scope);
          return new CachedValueProvider.Result<M>(combinedModel, computeDependencies(combinedModel, scope));
        }
      };

      myAllModelsCache = new DomModelCache<List<M>, Scope>(project, name + " models list") {
        @Nonnull
        protected CachedValueProvider.Result<List<M>> computeValue(@Nonnull final Scope scope) {
          final List<M> models = computeAllModels(scope);
          return new CachedValueProvider.Result<List<M>>(models, computeDependencies(null, scope));
        }
      };
    }

    @Nullable
    public abstract M getModel(@Nonnull C context);

    @Nonnull
    public List<M> getAllModels(@Nonnull Scope scope) {

      final List<M> models = myAllModelsCache.getCachedValue(scope);
      if (models == null) {
        return Collections.emptyList();
      }
      else {
        return models;
      }
    }

    @Nullable
    protected abstract List<M> computeAllModels(@Nonnull Scope scope);

    @Nullable
    public M getCombinedModel(@Nullable Scope scope) {
      if (scope == null) {
        return null;
      }
      return myCombinedModelCache.getCachedValue(scope);
    }

    @Nullable
    protected M computeCombinedModel(@Nonnull Scope scope) {
      final List<M> models = getAllModels(scope);
      switch (models.size()) {
        case 0:
          return null;
        case 1:
          return models.get(0);
      }
      final Set<XmlFile> configFiles = new LinkedHashSet<XmlFile>();
      final LinkedHashSet<DomFileElement<T>> list = new LinkedHashSet<DomFileElement<T>>(models.size());
      for (M model: models) {
        final Set<XmlFile> files = model.getConfigFiles();
        for (XmlFile file: files) {
          ContainerUtil.addIfNotNull(list, getDomRoot(file));
        }
        configFiles.addAll(files);
      }
      final DomFileElement<T> mergedModel = getModelMerger().mergeModels(DomFileElement.class, list);
      final M firstModel = models.get(0);
      return createCombinedModel(configFiles, mergedModel, firstModel, scope);
    }

    /**
     * Factory method to create combined model for given module.
     * Used by {@link #computeCombinedModel(Module)}.
     *
     * @param configFiles file set including all files for all models returned by {@link #getAllModels(Module)}.
     * @param mergedModel merged model for all models returned by {@link #getAllModels(Module)}.
     * @param firstModel the first model returned by {@link #getAllModels(Module)}.
     * @param scope
     * @return combined model.
     */
    protected abstract M createCombinedModel(Set<XmlFile> configFiles, DomFileElement<T> mergedModel, M firstModel, final Scope scope);

    @Nonnull
    public Set<XmlFile> getConfigFiles(@Nullable C context) {
      if (context == null) {
        return Collections.emptySet();
      }
      final M model = getModel(context);
      if (model == null) {
        return Collections.emptySet();
      }
      else {
        return model.getConfigFiles();
      }
    }

    @Nonnull
    public Set<XmlFile> getAllConfigFiles(@Nonnull Scope scope) {
      final HashSet<XmlFile> xmlFiles = new HashSet<XmlFile>();
      for (M model: getAllModels(scope)) {
        xmlFiles.addAll(model.getConfigFiles());
      }
      return xmlFiles;
    }

    public List<DomFileElement<T>> getFileElements(M model) {
      final ArrayList<DomFileElement<T>> list = new ArrayList<DomFileElement<T>>(model.getConfigFiles().size());
      for (XmlFile configFile: model.getConfigFiles()) {
        final DomFileElement<T> element = DomManager.getDomManager(configFile.getProject()).getFileElement(configFile, myClass);
        if (element != null) {
          list.add(element);
        }
      }
      return list;
    }

  }
