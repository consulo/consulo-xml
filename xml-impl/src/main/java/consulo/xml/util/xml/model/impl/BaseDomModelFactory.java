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

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.module.content.ProjectRootManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomService;
import consulo.xml.util.xml.ModelMerger;
import consulo.xml.util.xml.model.DomModel;
import consulo.xml.util.xml.model.MultipleDomModelFactory;
import consulo.xml.util.xml.model.SimpleModelFactory;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.UserDataHolder;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Sergey.Vasiliev
 */
public abstract class BaseDomModelFactory<S extends UserDataHolder, T extends DomElement, M extends DomModel<T>, C extends PsiElement>
    extends DomModelFactoryHelper<T> implements SimpleModelFactory<T, M>, MultipleDomModelFactory<S, T, M> {

  private final Project myProject;
  private final SimpleModelFactory<T,M> mySimpleDomModelFactory;
  private final MultipleDomModelFactory<S, T, M> myMultipleDomModelFactory;

  protected BaseDomModelFactory(Class<T> aClass, final Project project, String name) {
    super(aClass, DomService.getInstance().createModelMerger());

    myProject = project;

    mySimpleDomModelFactory = createSimpleModelFactory(aClass, getModelMerger(), project, name);

    myMultipleDomModelFactory = createMultipleDomModelFactory(aClass, getModelMerger(), project, name);
  }

  protected abstract S getModelScope(final XmlFile file);

  @Nullable
  protected abstract List<M> computeAllModels(S scope);

  protected abstract M createCombinedModel(Set<XmlFile> configFiles, DomFileElement<T> mergedModel, M firstModel, final S scope);

  @Nullable
  public M getModel(C context){
    final PsiFile psiFile = context.getContainingFile();
    if (psiFile instanceof XmlFile) {
      return getModelByConfigFile((XmlFile)psiFile);
    }
    return null;
  }

  public List<M> getAllModels(S scope) {
    return myMultipleDomModelFactory.getAllModels(scope);
  }

  @Nullable
  public M getModelByConfigFile(@Nullable XmlFile psiFile) {
    return mySimpleDomModelFactory.getModelByConfigFile(psiFile);
  }

  public Object[] computeDependencies(@Nullable M model, @Nullable S scope) {

    final ArrayList<Object> dependencies = new ArrayList<Object>();
    dependencies.add(PsiModificationTracker.MODIFICATION_COUNT);
    if (scope != null) {
      dependencies.add(ProjectRootManager.getInstance(getProject()));
    }
    return ArrayUtil.toObjectArray(dependencies);
  }

  @Nullable
  protected M computeModel(XmlFile psiFile, @Nullable S scope) {
    if (scope == null) {
      return null;
    }
    final List<M> models = getAllModels(scope);
    for (M model : models) {
      final Set<XmlFile> configFiles = model.getConfigFiles();
      if (configFiles.contains(psiFile)) {
        return model;
      }
    }
    return null;
  }

  @Nullable
  public M getCombinedModel(@Nullable S scope) {
    return myMultipleDomModelFactory.getCombinedModel(scope);
  }

  public Set<XmlFile> getAllConfigFiles(S scope) {
    return myMultipleDomModelFactory.getAllConfigFiles(scope);
  }

  @Nullable
  public DomFileElement<T> createMergedModelRoot(final Set<XmlFile> configFiles) {
    return mySimpleDomModelFactory.createMergedModelRoot(configFiles);
  }

  private CachedMultipleDomModelFactory<S, T, M, C> createMultipleDomModelFactory(final Class<T> aClass,
                                                                                  final ModelMerger modelMerger,
                                                                                  final Project project,
                                                                                  final String name) {
    return new CachedMultipleDomModelFactory<S, T, M, C>(aClass, modelMerger, project, name) {
      public M getModel(final C context) {
        return BaseDomModelFactory.this.getModel(context);
      }

      protected List<M> computeAllModels(final S scope) {
        return BaseDomModelFactory.this.computeAllModels(scope);
      }

      protected M createCombinedModel(final Set<XmlFile> configFiles,
                                      final DomFileElement<T> mergedModel,
                                      final M firstModel,
                                      final S scope) {
        return BaseDomModelFactory.this.createCombinedModel(configFiles, mergedModel, firstModel, scope);
      }

      public Object[] computeDependencies(@Nullable final M model, @Nullable final S scope) {
        return BaseDomModelFactory.this.computeDependencies(model, scope);
      }

      public S getModelScope(final XmlFile xmlFile) {
        return BaseDomModelFactory.this.getModelScope(xmlFile);
      }
    };
  }

  private CachedSimpleDomModelFactory<T, M, S> createSimpleModelFactory(final Class<T> aClass,
                                                                        final ModelMerger modelMerger,
                                                                        final Project project,
                                                                        final String name) {
    return new CachedSimpleDomModelFactory<T, M, S>(aClass, modelMerger, project, name) {

      protected M computeModel(final XmlFile psiFile, @Nullable final S scope) {
        return BaseDomModelFactory.this.computeModel(psiFile, scope);
      }

      public Object[] computeDependencies(@Nullable final M model, @Nullable final S scope) {
        return BaseDomModelFactory.this.computeDependencies(model, scope);
      }

      public S getModelScope(XmlFile file) {
        return BaseDomModelFactory.this.getModelScope(file);
      }
    };
  }

  public Project getProject() {
    return myProject;
  }
}
