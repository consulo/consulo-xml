package consulo.xml.javaee;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import jakarta.inject.Singleton;

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
public class ApplicationExternalResourceManagerImpl extends ExternalResourceManagerExImpl
{
}
