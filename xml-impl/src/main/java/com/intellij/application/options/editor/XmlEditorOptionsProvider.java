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

import com.intellij.openapi.options.BeanConfigurable;
import com.intellij.openapi.options.Configurable;

/**
 * @author VISTALL
 */
public class XmlEditorOptionsProvider extends BeanConfigurable<XmlEditorOptions> implements Configurable
{
	public XmlEditorOptionsProvider()
	{
		super(XmlEditorOptions.getInstance());
		checkBox("automaticallyInsertClosingTag", "Automatically insert closing tag");
		checkBox("automaticallyInsertRequiredAttributes", "Automatically insert required attributes");
		checkBox("automaticallyStartAttribute", "Automatically start attribute");
		checkBox("automaticallyInsertRequiredSubTags", "Automatically insert required subtags");
	}


	@Override
	public String getDisplayName()
	{
		return null;
	}

	@Override
	public String getHelpTopic()
	{
		return null;
	}
}
