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

import javax.annotation.Nullable;

import org.jetbrains.annotations.Nls;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.BeanConfigurable;
import com.intellij.openapi.options.Configurable;

/**
 * @author Dmitry Avdeev
 */
public class XmlAutoImportOptionsProvider extends BeanConfigurable<XmlSettings> implements Configurable
{
	public XmlAutoImportOptionsProvider()
	{
		super(XmlSettings.getInstance());
		checkBox("SHOW_XML_ADD_IMPORT_HINTS", ApplicationBundle.message("checkbox.show.import.popup"));
	}

	@Nls
	@Override
	public String getDisplayName()
	{
		return null;
	}

	@Nullable
	@Override
	public String getHelpTopic()
	{
		return null;
	}
}
