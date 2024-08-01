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

/*
 * User: anna
 * Date: 14-Feb-2008
 */
package consulo.xml.application.options.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionImpl
public class XmlCodeFoldingConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
    @RequiredUIAccess
    @Nonnull
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();

        XmlFoldingSettings settings = XmlFoldingSettings.getInstance();

        CheckBox collapseXmlTags = CheckBox.create(ApplicationLocalize.checkboxCollapseXmlTags());
        layout.add(collapseXmlTags);
        propertyBuilder.add(collapseXmlTags, settings::isCollapseXmlTags, settings::setCollapseXmlTags);

        return layout;
    }

    @Nonnull
    @Override
    public String getId() {
        return "editor.preferences.folding.xml";
    }

    @Nullable
    @Override
    public String getParentId() {
        return "editor.preferences.folding";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "XML";
    }
}