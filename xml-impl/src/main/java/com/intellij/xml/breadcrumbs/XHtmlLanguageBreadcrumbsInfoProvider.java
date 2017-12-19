package com.intellij.xml.breadcrumbs;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.Language;
import com.intellij.lang.xhtml.XHTMLLanguage;

/**
 * @author VISTALL
 * @since 07.12.2015
 */
public class XHtmlLanguageBreadcrumbsInfoProvider extends HtmlLanguageBreadcrumbsInfoProvider
{
	@NotNull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
