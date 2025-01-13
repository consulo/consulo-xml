/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.intelliLang.inject.config.ui.configurables;

import consulo.language.inject.advanced.ui.InjectionConfigurable;
import consulo.project.Project;
import consulo.xml.intelliLang.inject.config.XmlAttributeInjection;
import consulo.xml.intelliLang.inject.config.ui.XmlAttributePanel;

public class XmlAttributeInjectionConfigurable extends InjectionConfigurable<XmlAttributeInjection, XmlAttributePanel>
{
	public XmlAttributeInjectionConfigurable(XmlAttributeInjection injection, Runnable treeUpdater, Project project)
	{
		super(injection, treeUpdater, project);
	}

	protected XmlAttributePanel createOptionsPanelImpl()
	{
		return new XmlAttributePanel(myInjection, myProject);
	}

	public String getBannerSlogan()
	{
		return "Edit XML Attribute Injection";
	}
}
