package consulo.xml.util.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ModuleContextProvider {
  public static final ExtensionPointName<ModuleContextProvider> EP_NAME = ExtensionPointName.create(ModuleContextProvider.class);

  @Nonnull
  public abstract Module[] getContextModules(@Nonnull PsiFile context);

  public static Module[] getModules(@Nullable PsiFile context) {
    if (context == null) return Module.EMPTY_ARRAY;

    final Set<consulo.module.Module> modules = new HashSet<>();
    for (ModuleContextProvider moduleContextProvider : EP_NAME.getExtensionList()) {
      ContainerUtil.addAllNotNull(modules, moduleContextProvider.getContextModules(context));
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(context);
    if (module != null) modules.add(module);

    return modules.toArray(new Module[modules.size()]);
  }
}
