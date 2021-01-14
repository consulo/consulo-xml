package consulo.xml.navbar;

import com.intellij.ide.navigationToolbar.AbstractNavBarModelExtension;
import com.intellij.ide.ui.UISettings;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import consulo.annotation.access.RequiredReadAction;

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
