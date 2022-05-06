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

import javax.annotation.Nonnull;

import com.intellij.codeInspection.XmlSuppressableInspectionTool;
import com.intellij.codeInspection.XmlInspectionGroupNames;
import com.intellij.xml.XmlBundle;
import com.intellij.xml.impl.ExternalDocumentValidator;
import consulo.language.editor.inspection.scheme.UnfairLocalInspectionTool;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import org.jetbrains.annotations.NonNls;

/**
 * @author Maxim Mossienko
 * @see ExternalDocumentValidator
 */
public class CheckXmlFileWithXercesValidatorInspection extends XmlSuppressableInspectionTool implements UnfairLocalInspectionTool {
  public static final @NonNls String SHORT_NAME = "CheckXmlFileWithXercesValidator";

  public boolean isEnabledByDefault() {
    return true;
  }

  @Nonnull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @Nonnull
  public String getGroupDisplayName() {
    return XmlInspectionGroupNames.XML_INSPECTIONS;
  }

  @Nonnull
  public String getDisplayName() {
    return XmlBundle.message("xml.inspections.check.file.with.xerces");
  }

  @Nonnull
  @NonNls
  public String getShortName() {
    return SHORT_NAME;
  }
}