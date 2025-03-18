package org.intellij.plugins.relaxNG;

import com.intellij.xml.util.HtmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.xml.javaee.ResourceRegistrar;
import consulo.xml.javaee.StandardResourceProvider;

/**
* @author VISTALL
* @since 31-Jul-22
*/
@ExtensionImpl
public class RelaxNGResourceProvider implements StandardResourceProvider
{
	@Override
	public void registerResources(ResourceRegistrar registrar)
	{
		registrar.addStdResource(ApplicationLoader.RNG_NAMESPACE, "/resources/relaxng.rng");
        registrar.addStdResource(HtmlUtil.SVG_NAMESPACE, "resources/html5-schema/svg20/svg20.rnc");
        registrar.addStdResource(HtmlUtil.MATH_ML_NAMESPACE, "resources/html5-schema/mml3/mathml3.rnc");

		registrar.addIgnoredResource("http://relaxng.org/ns/compatibility/annotations/1.0");
	}
}
