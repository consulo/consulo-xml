package com.intellij.javaee;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.xml.Html5SchemaProvider;
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
