package com.intellij.ide.highlighter;

import javax.annotation.Nonnull;

import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;

/**
 * @author VISTALL
 * @since 14.02.2015
 */
public class XmlSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory
{
	@Nonnull
	@Override
	protected SyntaxHighlighter createHighlighter()
	{
		return new XmlFileHighlighter();
	}
}
