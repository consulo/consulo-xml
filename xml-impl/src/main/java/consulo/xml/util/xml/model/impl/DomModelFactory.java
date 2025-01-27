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
import consulo.module.Module;
import consulo.project.Project;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.model.DomModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class DomModelFactory<T extends DomElement, M extends DomModel<T>, C extends PsiElement> extends BaseDomModelFactory<Module, T, M, C> {

  protected DomModelFactory(@Nonnull Class<T> aClass, final Project project, @NonNls String name) {
    super(aClass, project, name);
  }

  protected Module getModelScope(final XmlFile file) {
    return file.getModule();
  }

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

  public List<DomFileElement<T>> getFileElements(M model) {
    final ArrayList<DomFileElement<T>> list = new ArrayList<DomFileElement<T>>(model.getConfigFiles().size());
    for (XmlFile configFile : model.getConfigFiles()) {
      final DomFileElement<T> element = DomManager.getDomManager(configFile.getProject()).getFileElement(configFile, myClass);
      if (element != null) {
        list.add(element);
      }
    }
    return list;
  }
}
