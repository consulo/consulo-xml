/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.xml.codeInspection.htmlInspections;

import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.inspection.UnfairLocalInspectionTool;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.localize.XmlLocalize;
import jakarta.annotation.Nonnull;

public abstract class RequiredAttributesInspectionBase extends XmlSuppressableInspectionTool implements UnfairLocalInspectionTool {
    protected static final Logger LOG = Logger.getInstance(RequiredAttributesInspectionBase.class);

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return XmlInspectionGroupNames.HTML_INSPECTIONS;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return InspectionLocalize.inspectionRequiredAttributesDisplayName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return XmlEntitiesInspection.REQUIRED_ATTRIBUTES_SHORT_NAME;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    public InspectionToolState<?> createStateProvider() {
        return new XmlEntitiesInspectionState(InspectionLocalize.inspectionJavadocHtmlNotRequiredLabelText());
    }

    public static IntentionAction getIntentionAction(@Nonnull String name) {
        return new AddHtmlTagOrAttributeToCustomsIntention(
            XmlEntitiesInspection.REQUIRED_ATTRIBUTES_SHORT_NAME,
            name,
            XmlLocalize.addOptionalHtmlAttribute(name)
        );
    }
}
