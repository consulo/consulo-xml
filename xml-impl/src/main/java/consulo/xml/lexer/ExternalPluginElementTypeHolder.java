package consulo.xml.lexer;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.PerApplicationInstance;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.lang.HtmlInlineScriptTokenTypesProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-08-07
 *
 * @see ExternalPluginHelper
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Deprecated
@DeprecationInfo("Find another way, this create hard reference to other plugins")
public class ExternalPluginElementTypeHolder
{
	private static PerApplicationInstance<ExternalPluginElementTypeHolder> ourInstance = PerApplicationInstance.of(ExternalPluginElementTypeHolder.class);

	@Nonnull
	public static ExternalPluginElementTypeHolder getInstance()
	{
		return ourInstance.get();
	}

	private final IElementType myInlineStyleElementType;
	private final IElementType myInlineScriptElementType;
	private final FileType myInlineScriptFileType;

	@Inject
	ExternalPluginElementTypeHolder()
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

		Language javaScriptLanguage = ExternalPluginHelper.getJavaScriptLanguage();
		HtmlInlineScriptTokenTypesProvider provider = javaScriptLanguage == null ? null : HtmlInlineScriptTokenTypesProvider.forLanguage(javaScriptLanguage);
		myInlineScriptElementType = provider != null ? provider.getElementType() : null;
		myInlineScriptFileType = provider == null ? null : provider.getFileType();
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

	@Nullable
	public FileType getInlineScriptFileType()
	{
		return myInlineScriptFileType;
	}
}
