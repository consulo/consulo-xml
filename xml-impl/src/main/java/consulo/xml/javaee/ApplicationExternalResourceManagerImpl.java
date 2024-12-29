package consulo.xml.javaee;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.xml.impl.internal.StandardExternalResourceData;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@Singleton
@State(name = "ExternalResourceManagerImpl", storages = {
		@Storage(file = StoragePathMacros.APP_CONFIG + "/javaeeExternalResources.xml"),
		@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true)
})
@ServiceImpl
public class ApplicationExternalResourceManagerImpl extends ExternalResourceManagerExImpl implements ApplicationExternalResourceManager
{
	private final Application myApplication;

	@Inject
	public ApplicationExternalResourceManagerImpl(Application application)
	{
		myApplication = application;
	}

	@Nonnull
	@Override
	protected StandardExternalResourceData getData()
	{
		ExtensionPoint<StandardResourceProvider> point = myApplication.getExtensionPoint(StandardResourceProvider.class);
		return point.getOrBuildCache(StandardExternalResourceData.CACHE_KEY);
	}
}
