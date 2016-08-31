package com.intellij.xml.breadcrumbs;

import java.util.StringTokenizer;

import org.jetbrains.annotations.NotNull;
import com.intellij.application.options.editor.XmlEditorOptions;
import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import consulo.annotations.RequiredReadAction;

/**
 * @author VISTALL
 * @since 07.12.2015
 */
public class HtmlLanguageBreadcrumbsInfoProvider extends XmlLanguageBreadcrumbsInfoProvider
{
	@Override
	public boolean validateFileProvider(@NotNull FileViewProvider fileViewProvider)
	{
		return XmlEditorOptions.getInstance().isBreadcrumbsEnabled();
	}

	@RequiredReadAction
	@Override
	@NotNull
	public String getElementInfo(@NotNull final PsiElement e)
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

	@NotNull
	@Override
	public Language getLanguage()
	{
		return HTMLLanguage.INSTANCE;
	}
}
