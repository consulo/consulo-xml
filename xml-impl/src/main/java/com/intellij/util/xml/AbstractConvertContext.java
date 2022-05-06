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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.language.psi.PsiManager;
import consulo.module.content.ProjectRootManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author peter
 */
public abstract class AbstractConvertContext extends ConvertContext {

  public final XmlTag getTag() {
    return getInvocationElement().getXmlTag();
  }

  @Nullable
  public XmlElement getXmlElement() {
    return getInvocationElement().getXmlElement();
  }

  @Nonnull
  public final XmlFile getFile() {
    return DomUtil.getFile(getInvocationElement());
  }

  public consulo.module.Module getModule() {
    final DomFileElement<DomElement> fileElement = DomUtil.getFileElement(getInvocationElement());
    if (fileElement == null) {
      final XmlElement xmlElement = getInvocationElement().getXmlElement();
      return xmlElement == null ? null : ModuleUtil.findModuleForPsiElement(xmlElement);
    }
    return fileElement.getRootElement().getModule();
  }

  public PsiManager getPsiManager() {
    return getFile().getManager();
  }

  @Nullable
  public GlobalSearchScope getSearchScope() {
    GlobalSearchScope scope = null;

    consulo.module.Module[] modules = getConvertContextModules(this);
    if (modules.length != 0) {

      PsiFile file = getFile();
      file = file.getOriginalFile();
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile != null) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(file.getProject()).getFileIndex();
        boolean tests = fileIndex.isInTestSourceContent(virtualFile);

        for (consulo.module.Module module : modules) {
          if (scope == null) {
            scope = GlobalSearchScope.moduleRuntimeScope(module, tests);
          }
          else {
            scope = scope.union(GlobalSearchScope.moduleRuntimeScope(module, tests));
          }
        }
      }
    }
    return scope; // ??? scope == null ? GlobalSearchScope.allScope(getProject()) : scope; ???
  }

  public static GlobalSearchScope getSearchScope(@Nonnull ConvertContext context) {
    consulo.module.Module[] modules = getConvertContextModules(context);
    if (modules.length == 0) return null;

    PsiFile file = context.getFile();
    file = file.getOriginalFile();
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(file.getProject()).getFileIndex();
    boolean tests = fileIndex.isInTestSourceContent(virtualFile);


    GlobalSearchScope scope = null;
    for (consulo.module.Module module : modules) {
      if (scope == null) {
        scope = GlobalSearchScope.moduleRuntimeScope(module, tests);
      }
      else {
        scope.union(GlobalSearchScope.moduleRuntimeScope(module, tests));
      }
    }
    return scope;
  }


  @Nonnull
  private static Module[] getConvertContextModules(@Nonnull ConvertContext context) {
    consulo.module.Module[] modules = ModuleContextProvider.getModules(context.getFile());
    if (modules.length > 0) return modules;

    final consulo.module.Module module = context.getModule();
    if (module != null) return new consulo.module.Module[]{module};

    return new consulo.module.Module[0];
  }
}
