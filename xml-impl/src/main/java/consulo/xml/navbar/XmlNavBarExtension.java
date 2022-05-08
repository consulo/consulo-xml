package consulo.xml.navbar;

import consulo.application.ui.UISettings;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.application.util.function.Processor;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlTag;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.impl.idea.ide.navigationToolbar.AbstractNavBarModelExtension;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 14/01/2021
 */
public class XmlNavBarExtension extends AbstractNavBarModelExtension
{
	@Nullable
	@Override
	public String getPresentableText(Object object)
	{
		if(object instanceof XmlTag)
		{
			return ((XmlTag) object).getName();
		}
		return null;
	}

	@Override
	public boolean normalizeChildren()
	{
		return false;
	}

	@Override
	@RequiredReadAction
	public PsiElement getLeafElement(@Nonnull DataContext dataContext)
	{
		if(UISettings.getInstance().getShowMembersInNavigationBar())
		{
			PsiFile psiFile = dataContext.getData(CommonDataKeys.PSI_FILE);
			Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
			if(psiFile == null || editor == null)
			{
				return null;
			}
			PsiElement psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
			if(psiElement != null && psiElement.getLanguage() instanceof XMLLanguage)
			{
				return PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
			}
		}
		return null;
	}

	@Nullable
	@Override
	public PsiElement getParent(@Nonnull PsiElement psiElement)
	{
		if(psiElement instanceof XmlTag)
		{
			PsiElement parent = psiElement.getParent();
			if(parent instanceof XmlDocument)
			{
				return psiElement.getContainingFile();
			}
			return ((XmlTag) psiElement).getParentTag();
		}
		return null;
	}

	@Override
	public boolean processChildren(Object object, Object rootElement, Processor<Object> processor)
	{
		if(UISettings.getInstance().getShowMembersInNavigationBar())
		{
			if(object instanceof XmlTag)
			{
				XmlTag parentTag = ((XmlTag) object).getParentTag();
				if(parentTag != null)
				{
					XmlTag[] subTags = parentTag.getSubTags();
					for(XmlTag subTag : subTags)
					{
						if(!processor.process(subTag))
						{
							return false;
						}
					}
				}
			}
		}
		return true;
	}
}
