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
package consulo.xml.javaee;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

/**
 * @author Dmitry Avdeev
 * Date: 7/20/12
 */
@ExtensionImpl
public class XMLCatalogConfigurable implements ProjectConfigurable {
    private final Project myProject;

    private VerticalLayout myLayout;
    private FileChooserTextBoxBuilder.Controller myCatalogFileBox;

    @Inject
    public XMLCatalogConfigurable(@Nonnull Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public String getId() {
        return "xml.catalog";
    }

    @Nullable
    @Override
    public String getParentId() {
        return "preferences.externalResources";
    }

    @Nonnull
    @Nls
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("XML Catalog");
    }

    @Override
    public String getHelpTopic() {
        return "XML.Catalog.Dialog";
    }

    @RequiredUIAccess
    @Nullable
    @Override
    public Component createUIComponent(@Nonnull Disposable parentDisposable) {
        if (myLayout == null) {
            FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(myProject);
            builder.fileChooserDescriptor(new FileChooserDescriptor(true, false, false, false, false, false));
            builder.dialogTitle(LocalizeValue.localizeTODO("XML Catalog Properties File"));

            myCatalogFileBox = builder.build();

            myLayout = VerticalLayout.create();
            myLayout.add(LabeledBuilder.filled(LocalizeValue.localizeTODO("Catalog property file:"), myCatalogFileBox.getComponent()));
        }
        return myLayout;
    }

    @RequiredUIAccess
    @Nullable
    @Override
    public Component getPreferredFocusedUIComponent() {
        return myCatalogFileBox.getComponent();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myCatalogFileBox = null;
        myLayout = null;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        ExternalResourceManagerEx.getInstanceEx().setCatalogPropertiesFile(myCatalogFileBox.getValue());
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        myCatalogFileBox.setValue(ExternalResourceManagerEx.getInstanceEx().getCatalogPropertiesFile());
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return !StringUtil.notNullize(ExternalResourceManagerEx.getInstanceEx().getCatalogPropertiesFile()).equals(myCatalogFileBox.getValue());
    }
}
