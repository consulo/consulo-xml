package consulo.xml.javaee;

import com.intellij.xml.Html5SchemaProvider;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationPropertiesComponent;
import consulo.project.ProjectManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Eugene.Kudelevsky
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class DefaultHtmlDoctypeInitialConfigurator
{
	@Inject
	public DefaultHtmlDoctypeInitialConfigurator(ProjectManager projectManager)
	{
		ApplicationPropertiesComponent propertiesComponent = ApplicationPropertiesComponent.getInstance();

		if(!propertiesComponent.getBoolean("DefaultHtmlDoctype.MigrateToHtml5", false))
		{
			propertiesComponent.setValue("DefaultHtmlDoctype.MigrateToHtml5", Boolean.TRUE.toString());
			ExternalResourceManagerEx.getInstanceEx().setDefaultHtmlDoctype(Html5SchemaProvider.getHtml5SchemaLocation(), projectManager.getDefaultProject());
		}
	}
}
