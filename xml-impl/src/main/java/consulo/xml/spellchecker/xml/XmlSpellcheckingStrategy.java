package consulo.xml.spellchecker.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.psi.PsiElement;
import consulo.language.spellchecker.editor.inspection.SuppressibleSpellcheckingStrategy;
import consulo.xml.language.XMLLanguage;
import consulo.xml.dom.DomElement;
import consulo.xml.dom.DomUtil;


/**
 * @author Sergey Evdokimov
 */
@ExtensionImpl
public class XmlSpellcheckingStrategy extends SuppressibleSpellcheckingStrategy
{
	@Override
	public boolean isSuppressedFor(PsiElement element, String name)
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
	public SuppressQuickFix[] getSuppressActions(PsiElement element, String name)
	{
		return SuppressQuickFix.EMPTY_ARRAY;
	}

	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
