package org.intellij.plugins.relaxNG;

import consulo.annotation.component.ExtensionImpl;
import org.intellij.plugins.relaxNG.xml.dom.RngElement;

@ExtensionImpl
public class RngElementDescription extends RngDomFileDescription<RngElement>
{
	public RngElementDescription()
	{
		super(RngElement.class, "element");
	}
}
