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
package com.intellij.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Provides custom tag names.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface XmlTagNameProvider {
    ExtensionPointName<XmlTagNameProvider> EP_NAME = ExtensionPointName.create(XmlTagNameProvider.class);

    void addTagNameVariants(List<LookupElement> elements, @Nonnull XmlTag tag, String prefix);
}
