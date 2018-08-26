package consulo.intelliLang.xml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiFile;

/**
 * @author VISTALL
 * @since 2018-08-26
 */
public interface XPathSupportProvider
{
	ExtensionPointName<XPathSupportProvider> EP_NAME = ExtensionPointName.create("com.intellij.xml.intelliLang.xpathSupportProvider");

	@Nullable
	static XPathSupportProvider findProvider()
	{
		for(XPathSupportProvider provider : EP_NAME.getExtensions())
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
