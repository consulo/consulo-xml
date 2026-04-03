package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;


@ExtensionImpl
public class RngSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory
{
	@Override
	protected SyntaxHighlighter createHighlighter()
	{
		return new RncHighlighter();
	}

	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
