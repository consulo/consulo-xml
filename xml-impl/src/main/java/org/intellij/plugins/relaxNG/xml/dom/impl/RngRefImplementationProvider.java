package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.dom.DomElementImplementationProvider;
import org.intellij.plugins.relaxNG.xml.dom.RngRef;


/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class RngRefImplementationProvider implements DomElementImplementationProvider<RngRef, RngRefImpl>
{
	@Override
	public Class<RngRef> getInterfaceClass()
	{
		return RngRef.class;
	}

	@Override
	public Class<RngRefImpl> getImplementationClass()
	{
		return RngRefImpl.class;
	}
}
