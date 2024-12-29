package consulo.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.Language;
import consulo.language.psi.PsiFile;
import org.jaxen.JaxenException;
import org.jaxen.XPath;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-26
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface XPathSupportProvider {
  ExtensionPointName<XPathSupportProvider> EP_NAME = ExtensionPointName.create(XPathSupportProvider.class);

  @Nullable
  static XPathSupportProvider findProvider() {
    for (XPathSupportProvider provider : EP_NAME.getExtensionList()) {
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
