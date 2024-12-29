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

package consulo.xml.codeInspection.htmlInspections;

import com.intellij.xml.XmlBundle;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlTagUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
@ExtensionImpl
public class HtmlExtraClosingTagInspection extends HtmlLocalInspectionTool {
  @Nullable
  @Override
  public Language getLanguage() {
    return XMLLanguage.INSTANCE;
  }

  @Override
  @Nls
  @Nonnull
  public String getDisplayName() {
    return XmlBundle.message("html.inspection.extra.closing.tag");
  }

  @Override
  @NonNls
  @Nonnull
  public String getShortName() {
    return "HtmlExtraClosingTag";
  }

  @Override
  @Nonnull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @Override
  protected void checkTag(@Nonnull final XmlTag tag, @Nonnull final ProblemsHolder holder, final boolean isOnTheFly, Object state) {
    final XmlToken endTagName = XmlTagUtil.getEndTagNameElement(tag);

    if (endTagName != null && tag instanceof HtmlTag && HtmlUtil.isSingleHtmlTag(tag.getName())) {
      holder.registerProblem(
        endTagName,
        XmlErrorLocalize.extraClosingTagForEmptyElement().get(),
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
        new RemoveExtraClosingTagIntentionAction()
      );
    }
  }
}
