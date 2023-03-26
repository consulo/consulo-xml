package consulo.xml.spellchecker.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.psi.PsiElement;
import consulo.language.spellchecker.editor.inspection.SuppressibleSpellcheckingStrategy;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomUtil;

import javax.annotation.Nonnull;

/**
 * @author Sergey Evdokimov
 */
@ExtensionImpl
public class XmlSpellcheckingStrategy extends SuppressibleSpellcheckingStrategy
{
	@Override
	public boolean isSuppressedFor(@Nonnull PsiElement element, @Nonnull String name)
	{
		DomElement domElement = DomUtil.getDomElement(element);
		if(domElement != null)
		{
			if(domElement.getAnnotation(NoSpellchecking.class) != null)
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public SuppressQuickFix[] getSuppressActions(@Nonnull PsiElement element, @Nonnull String name)
	{
		return SuppressQuickFix.EMPTY_ARRAY;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
