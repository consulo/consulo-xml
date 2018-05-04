package com.intellij.util.xml;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;

public abstract class ModuleContextProvider {
  public static final ExtensionPointName<ModuleContextProvider> EP_NAME = ExtensionPointName.create("com.intellij.xml.dom.moduleContextProvider");

  @Nonnull
  public abstract Module[] getContextModules(@Nonnull PsiFile context);

  public static Module[] getModules(@Nullable PsiFile context) {
    if (context == null) return Module.EMPTY_ARRAY;

    final Set<Module> modules = new HashSet<Module>();
    for (ModuleContextProvider moduleContextProvider : Extensions.getExtensions(EP_NAME)) {
      ContainerUtil.addAllNotNull(modules, moduleContextProvider.getContextModules(context));
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(context);
    if (module != null) modules.add(module);

    return modules.toArray(new Module[modules.size()]);
  }
}
