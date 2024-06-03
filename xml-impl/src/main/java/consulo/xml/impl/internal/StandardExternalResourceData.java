package consulo.xml.impl.internal;

import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionWalker;
import consulo.xml.javaee.StandardResourceProvider;

import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 03/06/2024
 */
public record StandardExternalResourceData(Map<String, Map<String, ExternalResource>> resources, Set<String> ignored)
{
	public static final ExtensionPointCacheKey<StandardResourceProvider, StandardExternalResourceData> CACHE_KEY = ExtensionPointCacheKey.create("ExternalResourceData",
			StandardExternalResourceData::build);

	public static StandardExternalResourceData build(ExtensionWalker<StandardResourceProvider> walker)
	{
		ResourceRegistrarImpl registrar = new ResourceRegistrarImpl();
		walker.walk(it -> registrar.withClassLoader(it.getClass().getClassLoader(), () -> it.registerResources(registrar)));
		Map<String, Map<String, ExternalResource>> resources = registrar.getResources();
		Set<String> ignored = registrar.getIgnored();
		return new StandardExternalResourceData(resources, ignored);
	}
}
