package consulo.xml.dom;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;

import org.jspecify.annotations.Nullable;
import java.util.HashSet;
import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ModuleContextProvider {
  public static final ExtensionPointName<ModuleContextProvider> EP_NAME = ExtensionPointName.create(ModuleContextProvider.class);

  public abstract Module[] getContextModules(PsiFile context);

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
