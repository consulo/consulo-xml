package consulo.xml.lexer;

import consulo.annotation.DeprecationInfo;
import consulo.component.util.pointer.NamedPointer;
import consulo.language.Language;
import consulo.language.LanguagePointerUtil;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 24/05/2023
 */
@Deprecated
@DeprecationInfo("Find another way, this create hard reference to other plugins")
public class ExternalPluginHelper
{
	private static final NamedPointer<Language> ourCSSLanguage = LanguagePointerUtil.createPointer("CSS");
	private static final NamedPointer<Language> ourJavaScriptLanguage = LanguagePointerUtil.createPointer("JavaScript");

	@Nullable
	public static Language getCssLanguage()
	{
		return ourCSSLanguage.get();
	}

	@Nullable
	public static Language getJavaScriptLanguage()
	{
		return ourJavaScriptLanguage.get();
	}
}
