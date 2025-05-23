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
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author spleaner
 */
@Singleton
@State(name = "XmlEditorOptions", storages = @Storage("editor.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class XmlEditorOptions implements PersistentStateComponent<XmlEditorOptions> {
    private boolean myAutomaticallyInsertClosingTag = true;
    private boolean myAutomaticallyInsertRequiredAttributes = true;
    private boolean myAutomaticallyInsertRequiredSubTags = true;
    private boolean myAutomaticallyStartAttribute = true;
    private boolean mySyncTagEditing = true;

    private boolean myInsertQuotesForAttributeValue = true;

    private boolean myTagTreeHighlightingEnabled = true;
    private int myTagTreeHighlightingLevelCount = 6;
    private int myTagTreeHighlightingOpacity = 10;

    @Nonnull
    public static XmlEditorOptions getInstance() {
        return Application.get().getInstance(XmlEditorOptions.class);
    }

    public XmlEditorOptions() {
        setTagTreeHighlightingEnabled(!Application.get().isUnitTestMode());
    }

    public boolean isAutomaticallyInsertClosingTag() {
        return myAutomaticallyInsertClosingTag;
    }

    public void setAutomaticallyInsertClosingTag(final boolean automaticallyInsertClosingTag) {
        myAutomaticallyInsertClosingTag = automaticallyInsertClosingTag;
    }

    public boolean isAutomaticallyInsertRequiredAttributes() {
        return myAutomaticallyInsertRequiredAttributes;
    }

    public void setAutomaticallyInsertRequiredAttributes(final boolean automaticallyInsertRequiredAttributes) {
        myAutomaticallyInsertRequiredAttributes = automaticallyInsertRequiredAttributes;
    }

    public boolean isAutomaticallyStartAttribute() {
        return myAutomaticallyStartAttribute;
    }

    public void setAutomaticallyStartAttribute(final boolean automaticallyStartAttribute) {
        myAutomaticallyStartAttribute = automaticallyStartAttribute;
    }

    public boolean isAutomaticallyInsertRequiredSubTags() {
        return myAutomaticallyInsertRequiredSubTags;
    }

    public void setAutomaticallyInsertRequiredSubTags(boolean automaticallyInsertRequiredSubTags) {
        myAutomaticallyInsertRequiredSubTags = automaticallyInsertRequiredSubTags;
    }

    public void setTagTreeHighlightingLevelCount(int tagTreeHighlightingLevelCount) {
        myTagTreeHighlightingLevelCount = tagTreeHighlightingLevelCount;
    }

    public int getTagTreeHighlightingLevelCount() {
        return myTagTreeHighlightingLevelCount;
    }

    public void setTagTreeHighlightingOpacity(int tagTreeHighlightingOpacity) {
        myTagTreeHighlightingOpacity = tagTreeHighlightingOpacity;
    }

    public int getTagTreeHighlightingOpacity() {
        return myTagTreeHighlightingOpacity;
    }

    public void setTagTreeHighlightingEnabled(boolean tagTreeHighlightingEnabled) {
        myTagTreeHighlightingEnabled = tagTreeHighlightingEnabled;
    }

    public boolean isTagTreeHighlightingEnabled() {
        return myTagTreeHighlightingEnabled;
    }

    public boolean isSyncTagEditing() {
        return mySyncTagEditing;
    }

    public void setSyncTagEditing(boolean syncTagEditing) {
        mySyncTagEditing = syncTagEditing;
    }

    public boolean isInsertQuotesForAttributeValue() {
        return myInsertQuotesForAttributeValue;
    }

    public void setInsertQuotesForAttributeValue(boolean insertQuotesForAttributeValue) {
        myInsertQuotesForAttributeValue = insertQuotesForAttributeValue;
    }

    @Nullable
    @Override
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public XmlEditorOptions getState() {
        return this;
    }

    @Override
    public void loadState(final XmlEditorOptions state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
