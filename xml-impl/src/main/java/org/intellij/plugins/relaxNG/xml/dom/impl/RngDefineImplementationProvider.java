package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.dom.DomElementImplementationProvider;
import org.intellij.plugins.relaxNG.xml.dom.RngDefine;


/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@ExtensionImpl
public class RngDefineImplementationProvider implements DomElementImplementationProvider<RngDefine, RngDefineImpl>
{
	@Override
	public Class<RngDefine> getInterfaceClass()
	{
		return RngDefine.class;
	}

	@Override
	public Class<RngDefineImpl> getImplementationClass()
	{
		return RngDefineImpl.class;
	}
}
