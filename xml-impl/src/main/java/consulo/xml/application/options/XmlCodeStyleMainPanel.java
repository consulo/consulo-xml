/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.xml.application.options;

import com.intellij.xml.arrangement.XmlArrangementPanel;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.setting.CodeStyleSettingsProvider;
import consulo.language.codeStyle.ui.setting.TabbedLanguageCodeStylePanel;
import consulo.xml.lang.xml.XMLLanguage;

/**
 * @author Rustam Vishnyakov
 */
public class XmlCodeStyleMainPanel extends TabbedLanguageCodeStylePanel
{
  protected XmlCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
    super(XMLLanguage.INSTANCE, currentSettings, settings);
  }

  @Override
  protected void initTabs(CodeStyleSettings settings) {
    addIndentOptionsTab(settings);
    addTab(new CodeStyleXmlPanel(settings));
    addTab(new XmlArrangementPanel(settings));

    for (CodeStyleSettingsProvider provider : CodeStyleSettingsProvider.EXTENSION_POINT_NAME.getExtensionList()) {
      if (provider.getLanguage() == XMLLanguage.INSTANCE && !provider.hasSettingsPage()) {
        createTab(provider);
      }
    }
  }
}
