package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.dom.DomElementImplementationProvider;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;


/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class RngGrammarImplementationProvider implements DomElementImplementationProvider<RngGrammar, RngGrammarImpl>
{
	@Override
	public Class<RngGrammar> getInterfaceClass()
	{
		return RngGrammar.class;
	}

	@Override
	public Class<RngGrammarImpl> getImplementationClass()
	{
		return RngGrammarImpl.class;
	}
}
