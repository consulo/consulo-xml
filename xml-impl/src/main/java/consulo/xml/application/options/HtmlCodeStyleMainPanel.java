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

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.ui.setting.TabbedLanguageCodeStylePanel;
import consulo.xml.lang.html.HTMLLanguage;

/**
 * @author Rustam Vishnyakov
 */
public class HtmlCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
    protected HtmlCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
        super(HTMLLanguage.INSTANCE, currentSettings, settings);
    }

    @Override
    protected void initTabs(CodeStyleSettings settings) {
        addIndentOptionsTab(settings);
        addTab(new CodeStyleHtmlPanel(settings));
    }
}
