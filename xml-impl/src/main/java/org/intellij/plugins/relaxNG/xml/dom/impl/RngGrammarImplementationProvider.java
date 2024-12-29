package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.dom.DomElementImplementationProvider;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class RngGrammarImplementationProvider implements DomElementImplementationProvider<RngGrammar, RngGrammarImpl>
{
	@Nonnull
	@Override
	public Class<RngGrammar> getInterfaceClass()
	{
		return RngGrammar.class;
	}

	@Nonnull
	@Override
	public Class<RngGrammarImpl> getImplementationClass()
	{
		return RngGrammarImpl.class;
	}
}
