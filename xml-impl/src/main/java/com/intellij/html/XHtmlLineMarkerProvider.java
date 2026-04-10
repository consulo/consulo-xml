package com.intellij.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.xhtml.XHTMLLanguage;


/**
 * @author VISTALL
 * @since 14-Jul-22
 */
@ExtensionImpl
public class XHtmlLineMarkerProvider extends HtmlLineMarkerProvider {
    @Override
    public Language getLanguage() {
        return XHTMLLanguage.INSTANCE;
    }
}
