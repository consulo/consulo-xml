package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.daemon.QuickFixActionRegistrar;
import com.intellij.codeInsight.daemon.impl.quickfix.FetchExtResourceAction;
import com.intellij.codeInsight.daemon.impl.quickfix.IgnoreExtResourceAction;
import com.intellij.codeInsight.daemon.impl.quickfix.ManuallySetupExtResourceAction;
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider;

/**
 * @author yole
 */
public class DependentNSReferenceQuickFixProvider extends UnresolvedReferenceQuickFixProvider<DependentNSReference>
{
	@Override
	public void registerFixes(@Nonnull DependentNSReference ref, @Nonnull QuickFixActionRegistrar registrar)
	{
		registrar.register(new FetchExtResourceAction(ref.isForceFetchResultValid()));
		registrar.register(new ManuallySetupExtResourceAction());
		registrar.register(new IgnoreExtResourceAction());
	}

	@Nonnull
	@Override
	public Class<DependentNSReference> getReferenceClass()
	{
		return DependentNSReference.class;
	}
}
