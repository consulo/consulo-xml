package com.intellij.ide.highlighter;

import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14.02.2015
 */
public class XHtmlSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory
{
	@Nonnull
	@Override
	protected SyntaxHighlighter createHighlighter()
	{
		return new XmlFileHighlighter(false, true);
	}
}
