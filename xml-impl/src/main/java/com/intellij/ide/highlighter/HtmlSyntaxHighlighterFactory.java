package com.intellij.ide.highlighter;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;

/**
 * @author VISTALL
 * @since 14.02.2015
 */
public class HtmlSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory
{
	@NotNull
	@Override
	protected SyntaxHighlighter createHighlighter()
	{
		return new HtmlFileHighlighter();
	}
}
