package consulo.xml.javaee;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import com.intellij.xml.Html5SchemaProvider;
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
		PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

		if(!propertiesComponent.getBoolean("DefaultHtmlDoctype.MigrateToHtml5", false))
		{
			propertiesComponent.setValue("DefaultHtmlDoctype.MigrateToHtml5", Boolean.TRUE.toString());
			ExternalResourceManagerEx.getInstanceEx().setDefaultHtmlDoctype(Html5SchemaProvider.getHtml5SchemaLocation(), projectManager.getDefaultProject());
		}
	}
}
