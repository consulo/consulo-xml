package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.dom.DomElementImplementationProvider;
import org.intellij.plugins.relaxNG.xml.dom.RngDomElement;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class RngDomElementImplementationProvider implements DomElementImplementationProvider<RngDomElement, RngDomElementBase>
{
	@Nonnull
	@Override
	public Class<RngDomElement> getInterfaceClass()
	{
		return RngDomElement.class;
	}

	@Nonnull
	@Override
	public Class<RngDomElementBase> getImplementationClass()
	{
		return RngDomElementBase.class;
	}
}
