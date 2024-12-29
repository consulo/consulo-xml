package org.intellij.plugins.relaxNG.compact;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class RngSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory
{
	@Override
	@Nonnull
	protected SyntaxHighlighter createHighlighter()
	{
		return new RncHighlighter();
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return RngCompactLanguage.INSTANCE;
	}
}
