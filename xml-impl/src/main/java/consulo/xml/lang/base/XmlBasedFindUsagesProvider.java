/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.lang.base;

import consulo.language.cacheBuilder.WordsScanner;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.meta.PsiMetaData;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;

/**
 * @author ven
 */
public abstract class XmlBasedFindUsagesProvider implements FindUsagesProvider
{
	@Override
	public boolean canFindUsagesFor(@Nonnull PsiElement element)
	{
		return element instanceof XmlElementDecl || element instanceof XmlAttributeDecl || element instanceof XmlEntityDecl || element instanceof XmlTag || element instanceof XmlAttributeValue ||
				element instanceof PsiFile || element instanceof XmlComment;
	}

	@Override
	@Nonnull
	public String getType(@Nonnull PsiElement element)
	{
		if(element instanceof XmlTag)
		{
			final PsiMetaData metaData = ((XmlTag) element).getMetaData();
			if(metaData != null && metaData.getDeclaration() instanceof XmlTag)
			{
				return ((XmlTag) metaData.getDeclaration()).getName();
			}
			return LanguageLocalize.xmlTermsXmlTag().get();
		}
		if(element instanceof XmlElementDecl)
		{
			return LanguageLocalize.xmlTermsTag().get();
		}
		else if(element instanceof XmlAttributeDecl)
		{
			return LanguageLocalize.xmlTermsAttribute().get();
		}
		else if(element instanceof XmlAttributeValue)
		{
			return LanguageLocalize.xmlTermsAttributeValue().get();
		}
		else if(element instanceof XmlEntityDecl)
		{
			return LanguageLocalize.xmlTermsEntity().get();
		}
		else if(element instanceof XmlAttribute)
		{
			return LanguageLocalize.xmlTermsAttribute().get();
		}
		else if(element instanceof XmlComment)
		{
			return LanguageLocalize.xmlTermsVariable().get();
		}
		throw new IllegalArgumentException("Cannot get type for " + element);
	}

	@Override
	@Nonnull
	public String getDescriptiveName(@Nonnull PsiElement element)
	{
		if(element instanceof XmlTag)
		{
			return ((XmlTag) element).getName();
		}

		if(element instanceof XmlAttributeValue)
		{
			return ((XmlAttributeValue) element).getValue();
		}

		if(element instanceof PsiNamedElement)
		{
			return ((PsiNamedElement) element).getName();
		}
		return element.getText();
	}

	@Override
	@Nonnull
	public String getNodeText(@Nonnull PsiElement element, boolean useFullName)
	{
		if(element instanceof XmlTag)
		{
			final XmlTag xmlTag = (XmlTag) element;
			final PsiMetaData metaData = xmlTag.getMetaData();
			final String name = metaData != null ? DescriptiveNameUtil.getMetaDataName(metaData) : xmlTag.getName();

			String presentableName = metaData == null ? "<" + name + ">" : name;
			return presentableName + " of file " + xmlTag.getContainingFile().getName();
		}
		if(element instanceof XmlAttributeValue)
		{
			return ((XmlAttributeValue) element).getValue();
		}
		if(element instanceof PsiNamedElement)
		{
			return ((PsiNamedElement) element).getName();
		}
		return element.getText();
	}

	@Override
	public WordsScanner getWordsScanner()
	{
		return null;
	}
}
