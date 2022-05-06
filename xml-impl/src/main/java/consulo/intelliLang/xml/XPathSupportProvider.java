package consulo.intelliLang.xml;

import consulo.component.extension.ExtensionPointName;
import consulo.language.Language;
import consulo.language.psi.PsiFile;
import org.jaxen.JaxenException;
import org.jaxen.XPath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-26
 */
public interface XPathSupportProvider
{
	ExtensionPointName<XPathSupportProvider> EP_NAME = ExtensionPointName.create("com.intellij.xml.xpathSupportProvider");

	@Nullable
	static XPathSupportProvider findProvider()
	{
		for(XPathSupportProvider provider : EP_NAME.getExtensionList())
		{
			return provider;
		}
		return null;
	}

	Language getLanguage();

	public static final Object UNSUPPORTED = "UNSUPPORTED";
	public static final Object INVALID = "INVALID";

	@Nonnull
	XPath createXPath(String expression) throws JaxenException;

	void attachContext(@Nonnull PsiFile file);
}
