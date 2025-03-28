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

package consulo.xml.application.options.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.xml.lang.XmlCodeFoldingSettings;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.ServiceManager;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "XmlFoldingSettings", storages = @Storage("editor.codeinsight.xml"))
public class XmlFoldingSettings implements XmlCodeFoldingSettings, PersistentStateComponent<XmlFoldingSettings> {
    public boolean COLLAPSE_XML_TAGS = false;
    public boolean COLLAPSE_HTML_STYLE_ATTRIBUTE = true;

    @Nonnull
    public static XmlFoldingSettings getInstance() {
        return ServiceManager.getService(XmlFoldingSettings.class);
    }

    @Override
    public boolean isCollapseXmlTags() {
        return COLLAPSE_XML_TAGS;
    }

    public void setCollapseXmlTags(boolean value) {
        COLLAPSE_XML_TAGS = value;
    }

    @Override
    public boolean isCollapseHtmlStyleAttribute() {
        return COLLAPSE_HTML_STYLE_ATTRIBUTE;
    }

    public void setCollapseHtmlStyleAttribute(boolean value) {
        this.COLLAPSE_HTML_STYLE_ATTRIBUTE = value;
    }

    @Override
    public XmlFoldingSettings getState() {
        return this;
    }

    @Override
    public void loadState(final XmlFoldingSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}