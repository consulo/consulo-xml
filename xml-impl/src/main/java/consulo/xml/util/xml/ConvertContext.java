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
package consulo.xml.util.xml;

import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public abstract class ConvertContext {

  @Nonnull
  public abstract DomElement getInvocationElement();

  @Nullable
  public abstract XmlTag getTag();

  @Nullable
  public abstract XmlElement getXmlElement();

  @Nullable
  public XmlElement getReferenceXmlElement() {
    final XmlElement element = getXmlElement();
    if (element instanceof XmlTag) {
      return element;
    }
    if (element instanceof XmlAttribute) {
      return ((XmlAttribute)element).getValueElement();
    }
    return null;
  }

  @Nonnull
  public abstract XmlFile getFile();

  @Nullable
  public abstract Module getModule();

  @Nullable
  public abstract GlobalSearchScope getSearchScope();
  
  public abstract PsiManager getPsiManager();

  public Project getProject() {
    return getPsiManager().getProject();
  }
}
