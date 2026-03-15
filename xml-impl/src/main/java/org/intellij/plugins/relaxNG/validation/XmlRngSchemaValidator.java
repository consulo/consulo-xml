package org.intellij.plugins.relaxNG.validation;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xml.lang.xml.XMLLanguage;


/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class XmlRngSchemaValidator extends RngSchemaValidator
{
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
