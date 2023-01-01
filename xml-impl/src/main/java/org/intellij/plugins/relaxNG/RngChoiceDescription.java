package org.intellij.plugins.relaxNG;

import consulo.annotation.component.ExtensionImpl;
import org.intellij.plugins.relaxNG.xml.dom.RngChoice;

@ExtensionImpl
public class RngChoiceDescription extends RngDomFileDescription<RngChoice>
{
	public RngChoiceDescription()
	{
		super(RngChoice.class, "choice");
	}
}
