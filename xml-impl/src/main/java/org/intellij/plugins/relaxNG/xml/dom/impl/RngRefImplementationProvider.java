package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.dom.DomElementImplementationProvider;
import org.intellij.plugins.relaxNG.xml.dom.RngRef;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class RngRefImplementationProvider implements DomElementImplementationProvider<RngRef, RngRefImpl>
{
	@Nonnull
	@Override
	public Class<RngRef> getInterfaceClass()
	{
		return RngRef.class;
	}

	@Nonnull
	@Override
	public Class<RngRefImpl> getImplementationClass()
	{
		return RngRefImpl.class;
	}
}
