package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.intention.QuickFixActionRegistrar;
import consulo.language.editor.intention.UnresolvedReferenceQuickFixProvider;
import consulo.xml.codeInsight.daemon.impl.quickfix.FetchExtResourceAction;
import consulo.xml.codeInsight.daemon.impl.quickfix.IgnoreExtResourceAction;
import consulo.xml.codeInsight.daemon.impl.quickfix.ManuallySetupExtResourceAction;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl
public class DependentNSReferenceQuickFixProvider extends UnresolvedReferenceQuickFixProvider<DependentNSReference> {
  @Override
  public void registerFixes(@Nonnull DependentNSReference ref, @Nonnull QuickFixActionRegistrar registrar) {
    registrar.register(new FetchExtResourceAction(ref.isForceFetchResultValid()));
    registrar.register(new ManuallySetupExtResourceAction());
    registrar.register(new IgnoreExtResourceAction());
  }

  @Nonnull
  @Override
  public Class<DependentNSReference> getReferenceClass() {
    return DependentNSReference.class;
  }
}
