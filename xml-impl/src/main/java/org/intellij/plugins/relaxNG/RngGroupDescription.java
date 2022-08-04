package org.intellij.plugins.relaxNG;

import consulo.annotation.component.ExtensionImpl;
import org.intellij.plugins.relaxNG.xml.dom.RngGroup;

@ExtensionImpl
public class RngGroupDescription extends RngDomFileDescription<RngGroup>
{
	public RngGroupDescription()
	{
		super(RngGroup.class, "group");
	}
}
