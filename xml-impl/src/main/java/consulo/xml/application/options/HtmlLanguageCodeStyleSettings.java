/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.setting.IndentOptionsEditor;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.codeStyle.ui.setting.SmartIndentOptionsEditor;
import consulo.xml.lang.html.HTMLLanguage;

import javax.annotation.Nonnull;

/**
 * @author Rustam Vishnyakov
 */
@ExtensionImpl
public class HtmlLanguageCodeStyleSettings extends LanguageCodeStyleSettingsProvider {
  @Nonnull
  @Override
  public Language getLanguage() {
    return HTMLLanguage.INSTANCE;
  }

  @Override
  public String getCodeSample(@Nonnull SettingsType settingsType) {
    return CodeStyleAbstractPanel.readFromFile(this.getClass(), "preview.html.template");
  }

  @Override
  public CommonCodeStyleSettings getDefaultCommonSettings() {
    CommonCodeStyleSettings defaultSettings = new CommonCodeStyleSettings(HTMLLanguage.INSTANCE);
    defaultSettings.initIndentOptions();
    return defaultSettings;
  }

  @Override
  public IndentOptionsEditor getIndentOptionsEditor() {
    return new SmartIndentOptionsEditor();
  }
}
