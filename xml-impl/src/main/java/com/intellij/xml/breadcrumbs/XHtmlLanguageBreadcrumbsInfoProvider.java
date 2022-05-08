package com.intellij.xml.breadcrumbs;

import javax.annotation.Nonnull;
import consulo.language.Language;
import consulo.xml.lang.xhtml.XHTMLLanguage;

/**
 * @author VISTALL
 * @since 07.12.2015
 */
public class XHtmlLanguageBreadcrumbsInfoProvider extends HtmlLanguageBreadcrumbsInfoProvider
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
