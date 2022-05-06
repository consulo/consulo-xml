package com.intellij.javaee;

import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import com.intellij.xml.Html5SchemaProvider;
import consulo.project.ProjectManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Eugene.Kudelevsky
 */
@Singleton
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
