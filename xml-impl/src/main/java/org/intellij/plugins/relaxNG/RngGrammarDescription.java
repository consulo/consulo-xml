package org.intellij.plugins.relaxNG;

import consulo.annotation.component.ExtensionImpl;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;

@ExtensionImpl
public class RngGrammarDescription extends RngDomFileDescription<RngGrammar>
{
	public RngGrammarDescription()
	{
		super(RngGrammar.class, "grammar");
	}
}
