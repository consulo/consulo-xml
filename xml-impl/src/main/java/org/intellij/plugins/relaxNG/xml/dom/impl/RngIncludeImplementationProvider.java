package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.dom.DomElementImplementationProvider;
import org.intellij.plugins.relaxNG.xml.dom.RngInclude;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class RngIncludeImplementationProvider implements DomElementImplementationProvider<RngInclude, RngIncludeImpl>
{
	@Nonnull
	@Override
	public Class<RngInclude> getInterfaceClass()
	{
		return RngInclude.class;
	}

	@Nonnull
	@Override
	public Class<RngIncludeImpl> getImplementationClass()
	{
		return RngIncludeImpl.class;
	}
}
