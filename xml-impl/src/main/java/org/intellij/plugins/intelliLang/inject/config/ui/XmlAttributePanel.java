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
package org.intellij.plugins.intelliLang.inject.config.ui;

import consulo.ide.impl.intelliLang.inject.config.ui.AbstractInjectionPanel;
import consulo.ide.impl.intelliLang.inject.config.ui.LanguagePanel;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.awt.LanguageTextField;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import org.intellij.plugins.intelliLang.inject.config.XmlAttributeInjection;

import javax.swing.*;

public class XmlAttributePanel extends AbstractInjectionPanel<XmlAttributeInjection>
{

	private JPanel myRoot;

	// read by reflection
	LanguagePanel myLanguagePanel;
	TagPanel myTagPanel;
	AdvancedXmlPanel myAdvancedPanel;

	private EditorTextField myLocalName;
	private ComboBox myNamespace;

	public XmlAttributePanel(XmlAttributeInjection injection, Project project)
	{
		super(injection, project);

		myNamespace.setModel(TagPanel.createNamespaceUriModel(project));

		init(injection.copy());

		// be sure to add the listener after initializing the textfield's value
		myLocalName.getDocument().addDocumentListener(new TreeUpdateListener());
	}

	@Override
	public JPanel getComponent()
	{
		return myRoot;
	}

	@Override
	protected void resetImpl()
	{
		myLocalName.setText(getOrigInjection().getAttributeName());
		myNamespace.getEditor().setItem(getOrigInjection().getAttributeNamespace());
	}

	@Override
	protected void apply(XmlAttributeInjection i)
	{
		i.setAttributeName(myLocalName.getText());
		i.setAttributeNamespace(getNamespace());
	}

	private String getNamespace()
	{
		final String s = (String) myNamespace.getEditor().getItem();
		return s != null ? s : "";
	}

	private void createUIComponents()
	{
		myLanguagePanel = new LanguagePanel(getProject(), getOrigInjection());
		myTagPanel = new TagPanel(getProject(), getOrigInjection());
		myAdvancedPanel = new AdvancedXmlPanel(getProject(), getOrigInjection());

		myLocalName = new LanguageTextField(RegExpLanguageDelegate.RegExp.get(), getProject(), getOrigInjection().getAttributeName());

		myNamespace = new ComboBox(200);
	}
}
