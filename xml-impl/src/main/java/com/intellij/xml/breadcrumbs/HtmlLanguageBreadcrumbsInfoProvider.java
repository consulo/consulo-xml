package com.intellij.xml.breadcrumbs;

import consulo.language.Language;
import com.intellij.lang.html.HTMLLanguage;
import consulo.language.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import consulo.annotation.access.RequiredReadAction;

import javax.annotation.Nonnull;
import java.util.StringTokenizer;

/**
 * @author VISTALL
 * @since 07.12.2015
 */
public class HtmlLanguageBreadcrumbsInfoProvider extends XmlLanguageBreadcrumbsInfoProvider
{
	@RequiredReadAction
	@Override
	@Nonnull
	public String getElementInfo(@Nonnull final PsiElement e)
	{
		final XmlTag tag = (XmlTag) e;
		final StringBuilder sb = new StringBuilder();

		sb.append(tag.getName());

		final String id_value = tag.getAttributeValue(ID_ATTRIBUTE_NAME);
		if(null != id_value)
		{
			sb.append("#").append(id_value);
		}

		final String class_value = tag.getAttributeValue(CLASS_ATTRIBUTE_NAME);
		if(null != class_value)
		{
			final StringTokenizer tokenizer = new StringTokenizer(class_value, " ");
			while(tokenizer.hasMoreTokens())
			{
				sb.append(".").append(tokenizer.nextToken());
			}
		}

		return sb.toString();
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
