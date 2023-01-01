package consulo.xml.javaee;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07-Sep-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ApplicationExternalResourceManager extends ExternalResourceManager
{
	@Nonnull
	public static ApplicationExternalResourceManager getInstance()
	{
		return Application.get().getInstance(ApplicationExternalResourceManager.class);
	}
}
