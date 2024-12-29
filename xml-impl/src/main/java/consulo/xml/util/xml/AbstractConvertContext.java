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

import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
      return xmlElement == null ? null : ModuleUtilCore.findModuleForPsiElement(xmlElement);
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
