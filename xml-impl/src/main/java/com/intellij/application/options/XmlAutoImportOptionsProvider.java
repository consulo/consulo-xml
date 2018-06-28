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
package com.intellij.application.options;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.Nls;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.Configurable;
import consulo.options.SimpleConfigurableByProperties;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.VerticalLayout;

/**
 * @author Dmitry Avdeev
 */
public class XmlAutoImportOptionsProvider extends SimpleConfigurableByProperties implements Configurable
{
	@RequiredUIAccess
	@Nonnull
	@Override
	protected Component createLayout(PropertyBuilder propertyBuilder)
	{
		VerticalLayout layout = VerticalLayout.create();

		XmlSettings settings = XmlSettings.getInstance();

		CheckBox showAddImports = CheckBox.create(ApplicationBundle.message("checkbox.show.import.popup"));
		layout.add(showAddImports);
		propertyBuilder.add(showAddImports, settings::isShowXmlImportsHints, settings::setShowXmlImportHints);

		return layout;
	}
}
