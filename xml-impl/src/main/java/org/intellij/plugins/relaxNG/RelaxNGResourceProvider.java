package org.intellij.plugins.relaxNG;

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
		registrar.addStdResource(ApplicationLoader.RNG_NAMESPACE, "/resources/relaxng.rng", getClass());
		registrar.addIgnoredResource("http://relaxng.org/ns/compatibility/annotations/1.0");
	}
}
