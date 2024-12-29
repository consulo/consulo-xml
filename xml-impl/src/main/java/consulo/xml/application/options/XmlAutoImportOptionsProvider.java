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
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class XmlAutoImportOptionsProvider extends SimpleConfigurableByProperties implements ApplicationConfigurable {
    @RequiredUIAccess
    @Nonnull
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();

        XmlSettings settings = XmlSettings.getInstance();

        CheckBox showAddImports = CheckBox.create(ApplicationLocalize.checkboxShowImportPopup());
        layout.add(showAddImports);
        propertyBuilder.add(showAddImports, settings::isShowXmlImportsHints, settings::setShowXmlImportHints);

        return layout;
    }

    @Nonnull
    @Override
    public String getId() {
        return "editor.preferences.import.xml";
    }

    @Nullable
    @Override
    public String getParentId() {
        return "editor.preferences.import";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "XML";
    }
}
