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
package com.intellij.xml.util;

import com.intellij.xml.impl.ExternalDocumentValidator;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.UnfairLocalInspectionTool;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Maxim Mossienko
 * @see ExternalDocumentValidator
 */
@ExtensionImpl
public class CheckXmlFileWithXercesValidatorInspection extends XmlSuppressableInspectionTool implements UnfairLocalInspectionTool {
    public static final String SHORT_NAME = "CheckXmlFileWithXercesValidator";

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nullable
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return XmlInspectionGroupNames.XML_INSPECTIONS;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return XmlLocalize.xmlInspectionsCheckFileWithXerces();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return SHORT_NAME;
    }
}