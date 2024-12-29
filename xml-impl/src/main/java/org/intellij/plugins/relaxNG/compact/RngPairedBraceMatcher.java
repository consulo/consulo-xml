package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class RngPairedBraceMatcher implements PairedBraceMatcher
{
	private BracePair[] myBracePairs;

	@Override
	public BracePair[] getPairs()
	{
		if(myBracePairs == null)
		{
			myBracePairs = new BracePair[]{
					new BracePair(RncTokenTypes.LBRACE, RncTokenTypes.RBRACE, true),
					new BracePair(RncTokenTypes.LPAREN, RncTokenTypes.RPAREN, false),
					new BracePair(RncTokenTypes.LBRACKET, RncTokenTypes.RBRACKET, false),
			};
		}
		return myBracePairs;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
