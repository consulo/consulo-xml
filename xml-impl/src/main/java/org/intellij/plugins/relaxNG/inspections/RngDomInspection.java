/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.relaxNG.inspections;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import consulo.xml.util.xml.highlighting.BasicDomElementsInspection;
import org.intellij.plugins.relaxNG.xml.dom.RngDomElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

/**
 * @author sweinreuter
 * @since 2007-10-03
 */
@ExtensionImpl
public class RngDomInspection extends BasicDomElementsInspection<RngDomElement, Object> {
    public RngDomInspection() {
        super(RngDomElement.class);
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return BaseInspection.getRngGroupDisplayName();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Unresolved References");
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "UnresolvedReference";
    }
}
