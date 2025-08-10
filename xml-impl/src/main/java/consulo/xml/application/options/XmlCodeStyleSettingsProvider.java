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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.Configurable;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;
import consulo.language.codeStyle.setting.CodeStyleSettingsProvider;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractConfigurable;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.localize.LocalizeValue;
import consulo.xml.psi.formatter.xml.XmlCodeStyleSettings;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl
public class XmlCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
    @Nonnull
    @Override
    public Configurable createSettingsPage(CodeStyleSettings settings, CodeStyleSettings originalSettings) {
        return new CodeStyleAbstractConfigurable(settings, originalSettings, ApplicationLocalize.titleXml()) {
            @Override
            protected CodeStyleAbstractPanel createPanel(final CodeStyleSettings settings) {
                return new XmlCodeStyleMainPanel(getCurrentSettings(), settings);
            }

            @Override
            public String getHelpTopic() {
                return "reference.settingsdialog.IDE.globalcodestyle.xml";
            }
        };
    }

    @Nonnull
    @Override
    public LocalizeValue getConfigurableDisplayName() {
        return ApplicationLocalize.titleXml();
    }

    @Override
    public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return new XmlCodeStyleSettings(settings);
    }
}
