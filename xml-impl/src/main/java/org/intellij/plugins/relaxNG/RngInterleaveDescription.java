package org.intellij.plugins.relaxNG;

import consulo.annotation.component.ExtensionImpl;
import org.intellij.plugins.relaxNG.xml.dom.RngInterleave;

@ExtensionImpl
public class RngInterleaveDescription extends RngDomFileDescription<RngInterleave>
{
	public RngInterleaveDescription()
	{
		super(RngInterleave.class, "interleave");
	}
}
