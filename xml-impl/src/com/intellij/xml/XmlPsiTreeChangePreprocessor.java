package com.intellij.xml;

import com.intellij.lang.xml.XMLLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.PsiTreeChangePreprocessorBase;

/**
 * @author VISTALL
 * @since 16.02.2015
 */
public class XmlPsiTreeChangePreprocessor extends PsiTreeChangePreprocessorBase
{
	public XmlPsiTreeChangePreprocessor(PsiManagerImpl psiManager)
	{
		super(psiManager);
	}

	@Override
	protected boolean isInsideCodeBlock(PsiElement element)
	{
		if(element instanceof PsiFileSystemItem)
		{
			return false;
		}

		if(element == null || element.getParent() == null)
		{
			return true;
		}

		final boolean isXml = element.getLanguage() instanceof XMLLanguage;
		// any xml element isn't inside a "code block"
		// cause we display even attributes and tag values in structure view
		return !isXml;
	}
}
