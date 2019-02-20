/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * User: anna
 * Date: 14-Feb-2008
 */
package com.intellij.application.options.editor;

import javax.annotation.Nonnull;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.Configurable;
import consulo.options.SimpleConfigurableByProperties;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

public class HtmlCodeFoldingConfigurable extends SimpleConfigurableByProperties implements Configurable
{
	@RequiredUIAccess
	@Nonnull
	@Override
	protected Component createLayout(PropertyBuilder propertyBuilder)
	{
		VerticalLayout layout = VerticalLayout.create();

		XmlFoldingSettings settings = XmlFoldingSettings.getInstance();

		CheckBox collapseHtmlStyles = CheckBox.create(ApplicationBundle.message("checkbox.collapse.html.style.attribute"));
		layout.add(collapseHtmlStyles);
		propertyBuilder.add(collapseHtmlStyles, settings::isCollapseHtmlStyleAttribute, settings::setCollapseHtmlStyleAttribute);

		return layout;
	}
}