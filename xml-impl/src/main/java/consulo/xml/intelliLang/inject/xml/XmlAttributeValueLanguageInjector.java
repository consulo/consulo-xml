package consulo.xml.intelliLang.inject.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.xml.psi.xml.XmlAttributeValue;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XmlAttributeValueLanguageInjector extends XmlLanguageInjector
{
	@Inject
	public XmlAttributeValueLanguageInjector(Project project)
	{
		super(project);
	}

	@Nonnull
	@Override
	public Class<? extends PsiElement> getElementClass()
	{
		return XmlAttributeValue.class;
	}
}
