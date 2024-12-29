package consulo.xml.intelliLang.inject.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.psi.injection.impl.ProjectInjectionConfiguration;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlAttributeValue;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XmlAttributeValueLanguageInjector extends XmlLanguageInjector
{
	@Inject
	public XmlAttributeValueLanguageInjector(ProjectInjectionConfiguration configuration)
	{
		super(configuration);
	}

	@Nonnull
	@Override
	public Class<? extends PsiElement> getElementClass()
	{
		return XmlAttributeValue.class;
	}
}
