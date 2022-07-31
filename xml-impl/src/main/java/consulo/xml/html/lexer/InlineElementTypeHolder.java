package consulo.xml.html.lexer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.PerApplicationInstance;
import consulo.language.ast.IElementType;
import consulo.xml.lang.HtmlInlineScriptTokenTypesProvider;
import consulo.xml.lexer.BaseHtmlLexer;
import consulo.xml.lexer.EmbeddedTokenTypesProvider;
import consulo.xml.lexer.HtmlLexer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-08-07
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class InlineElementTypeHolder
{
	private static PerApplicationInstance<InlineElementTypeHolder> ourInstance = PerApplicationInstance.of(InlineElementTypeHolder.class);

	@Nonnull
	public static InlineElementTypeHolder getInstance()
	{
		return ourInstance.get();
	}

	private final IElementType myInlineStyleElementType;
	private final IElementType myInlineScriptElementType;

	@Inject
	InlineElementTypeHolder()
	{
		List<EmbeddedTokenTypesProvider> extensions = EmbeddedTokenTypesProvider.EXTENSION_POINT_NAME.getExtensionList();
		IElementType inlineStyleElementType = null;
		for(EmbeddedTokenTypesProvider extension : extensions)
		{
			if(HtmlLexer.INLINE_STYLE_NAME.equals(extension.getName()))
			{
				inlineStyleElementType = extension.getElementType();
				break;
			}
		}
		myInlineStyleElementType = inlineStyleElementType;
		// At the moment only JS.
		HtmlInlineScriptTokenTypesProvider provider = BaseHtmlLexer.ourDefaultLanguage == null ? null : HtmlInlineScriptTokenTypesProvider.forLanguage(BaseHtmlLexer.ourDefaultLanguage);
		myInlineScriptElementType = provider != null ? provider.getElementType() : null;
	}

	@Nullable
	public IElementType getInlineScriptElementType()
	{
		return myInlineScriptElementType;
	}

	@Nullable
	public IElementType getInlineStyleElementType()
	{
		return myInlineStyleElementType;
	}
}
