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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.ServiceManager;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "XmlSettings", storages = @Storage("editor.codeinsight.xml"))
public class XmlSettings implements PersistentStateComponent<XmlSettings> {
    public boolean SHOW_XML_ADD_IMPORT_HINTS = true;

    public static XmlSettings getInstance() {
        return ServiceManager.getService(XmlSettings.class);
    }

    public void setShowXmlImportHints(boolean value) {
        SHOW_XML_ADD_IMPORT_HINTS = value;
    }

    public boolean isShowXmlImportsHints() {
        return SHOW_XML_ADD_IMPORT_HINTS;
    }

    @Override
    public XmlSettings getState() {
        return this;
    }

    @Override
    public void loadState(final XmlSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
