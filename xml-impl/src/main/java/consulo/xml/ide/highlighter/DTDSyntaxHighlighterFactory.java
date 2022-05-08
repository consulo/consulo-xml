package consulo.xml.ide.highlighter;

import javax.annotation.Nonnull;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;

/**
 * @author VISTALL
 * @since 14.02.2015
 */
public class DTDSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory
{
	@Nonnull
	@Override
	protected SyntaxHighlighter createHighlighter()
	{
		return new XmlFileHighlighter(true);
	}
}
