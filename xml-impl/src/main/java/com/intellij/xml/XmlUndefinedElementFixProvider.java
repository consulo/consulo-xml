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
package com.intellij.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class XmlUndefinedElementFixProvider {
    public static final ExtensionPointName<XmlUndefinedElementFixProvider> EP_NAME =
        ExtensionPointName.create(XmlUndefinedElementFixProvider.class);

    /**
     * @param tag
     * @return null if this provider doesn't know anything about this file; empty array if no fixes are available and no other
     * providers should be asked
     */
    @Nullable
    public IntentionAction[] createFixes(final @Nonnull XmlAttribute attribute) {
        return null;
    }

    /**
     * @param tag
     * @return null if this provider doesn't know anything about this file; empty array if no fixes are available and no other
     * providers should be asked
     */
    @Nullable
    public LocalQuickFix[] createFixes(final @Nonnull XmlTag tag) {
        return null;
    }
}
