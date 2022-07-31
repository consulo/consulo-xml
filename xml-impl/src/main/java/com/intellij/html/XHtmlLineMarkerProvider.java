package com.intellij.html;

import consulo.language.Language;
import consulo.xml.lang.xhtml.XHTMLLanguage;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Jul-22
 */
public class XHtmlLineMarkerProvider extends HtmlLineMarkerProvider
{
	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}
