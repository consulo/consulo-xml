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
package com.intellij.application.options.editor;

import com.intellij.openapi.options.Configurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.options.SimpleConfigurableByProperties;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 */
public class XmlEditorOptionsProvider extends SimpleConfigurableByProperties implements Configurable
{
	@RequiredUIAccess
	@Nonnull
	@Override
	protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable)
	{
		VerticalLayout layout = VerticalLayout.create();

		XmlEditorOptions options = XmlEditorOptions.getInstance();

		CheckBox automaticallyInsertClosingTag = CheckBox.create(LocalizeValue.localizeTODO("Automatically insert closing tag"));
		layout.add(automaticallyInsertClosingTag);
		propertyBuilder.add(automaticallyInsertClosingTag, options::isAutomaticallyInsertClosingTag, options::setAutomaticallyInsertClosingTag);

		CheckBox automaticallyInsertRequiredAttributes = CheckBox.create(LocalizeValue.localizeTODO("Automatically insert required attributes"));
		layout.add(automaticallyInsertRequiredAttributes);
		propertyBuilder.add(automaticallyInsertRequiredAttributes, options::isAutomaticallyInsertRequiredAttributes, options::setAutomaticallyInsertRequiredAttributes);

		CheckBox automaticallyStartAttribute = CheckBox.create(LocalizeValue.localizeTODO("Automatically start attribute"));
		layout.add(automaticallyStartAttribute);
		propertyBuilder.add(automaticallyStartAttribute, options::isAutomaticallyStartAttribute, options::setAutomaticallyStartAttribute);

		CheckBox automaticallyInsertRequiredSubTags = CheckBox.create(LocalizeValue.localizeTODO("Automatically insert required subtags"));
		layout.add(automaticallyInsertRequiredSubTags);
		propertyBuilder.add(automaticallyInsertRequiredSubTags, options::isAutomaticallyInsertRequiredSubTags, options::setAutomaticallyInsertRequiredSubTags);

		return layout;
	}
}
