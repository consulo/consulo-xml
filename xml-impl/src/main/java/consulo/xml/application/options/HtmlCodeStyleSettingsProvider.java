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
package consulo.xml.application.options;

import consulo.language.codeStyle.setting.CodeStyleSettingsProvider;
import consulo.application.ApplicationBundle;
import consulo.configurable.Configurable;
import consulo.ide.impl.idea.application.options.CodeStyleAbstractConfigurable;
import consulo.ide.impl.idea.application.options.CodeStyleAbstractPanel;
import consulo.language.codeStyle.CodeStyleSettings;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class HtmlCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  @Nonnull
  public Configurable createSettingsPage(final CodeStyleSettings settings, final CodeStyleSettings originalSettings) {
    return new CodeStyleAbstractConfigurable(settings, originalSettings, ApplicationBundle.message("title.html")) {
      protected CodeStyleAbstractPanel createPanel(final CodeStyleSettings settings) {
        return new HtmlCodeStyleMainPanel(settings, originalSettings);
      }

      public String getHelpTopic() {
        return "reference.settingsdialog.IDE.globalcodestyle.html";
      }
    };
  }

  @Override
  public String getConfigurableDisplayName() {
    return ApplicationBundle.message("title.html");
  }
}
