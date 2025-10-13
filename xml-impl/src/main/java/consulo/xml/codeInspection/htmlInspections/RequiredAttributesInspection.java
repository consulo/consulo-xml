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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.UnfairLocalInspectionTool;
import consulo.localize.LocalizeValue;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2005-11-18
 */
@ExtensionImpl
public class RequiredAttributesInspection extends RequiredAttributesInspectionBase implements UnfairLocalInspectionTool {
  @Nullable
  @Override
  public Language getLanguage() {
    return XMLLanguage.INSTANCE;
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return XmlLocalize.inspectionRequiredAttributesDisplayName();
  }
}
