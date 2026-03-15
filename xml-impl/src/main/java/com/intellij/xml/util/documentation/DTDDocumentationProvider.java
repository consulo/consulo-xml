package com.intellij.xml.util.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.dtd.DTDLanguage;


/**
 * @author VISTALL
 * @since 02-Aug-22
 */
@ExtensionImpl
public class DTDDocumentationProvider extends XmlDocumentationProvider {
    @Override
    public Language getLanguage() {
        return DTDLanguage.INSTANCE;
    }
}
