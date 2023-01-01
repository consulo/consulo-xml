package consulo.xml.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileTemplate.FileTemplateContributor;
import consulo.fileTemplate.FileTemplateManager;
import consulo.fileTemplate.FileTemplateRegistrator;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ExtensionImpl
public class HtmlInternalTemplateContributor implements FileTemplateContributor
{
	@Override
	public void register(@Nonnull FileTemplateRegistrator fileTemplateRegistrator)
	{
	 	fileTemplateRegistrator.registerInternalTemplate(FileTemplateManager.INTERNAL_HTML5_TEMPLATE_NAME);
	 	fileTemplateRegistrator.registerInternalTemplate(FileTemplateManager.INTERNAL_HTML_TEMPLATE_NAME);
	 	fileTemplateRegistrator.registerInternalTemplate(FileTemplateManager.INTERNAL_XHTML_TEMPLATE_NAME);
	}
}
