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

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.localize.LocalizeValue;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

public abstract class HtmlUnknownAttributeInspectionBase extends HtmlUnknownElementInspection {
  @Override
  @Nonnull
  public String getDisplayName() {
    return XmlLocalize.htmlInspectionsUnknownAttribute().get();
  }

  @Override
  @NonNls
  @Nonnull
  public String getShortName() {
    return XmlEntitiesInspection.ATTRIBUTE_SHORT_NAME;
  }

  @Override
  protected LocalizeValue getCheckboxTitle() {
    return XmlLocalize.htmlInspectionsUnknownTagAttributeCheckboxTitle();
  }

  @Override
  protected void checkAttribute(@Nonnull final XmlAttribute attribute,
                                @Nonnull final ProblemsHolder holder,
                                final boolean isOnTheFly,
                                Object state) {
    final XmlTag tag = attribute.getParent();

    if (tag instanceof HtmlTag) {
      XmlElementDescriptor elementDescriptor = tag.getDescriptor();
      if (elementDescriptor == null || elementDescriptor instanceof AnyXmlElementDescriptor) {
        return;
      }

      BaseHtmlEntitiesInspectionState toolState = (BaseHtmlEntitiesInspectionState)state;

      XmlAttributeDescriptor attributeDescriptor = elementDescriptor.getAttributeDescriptor(attribute);

      if (attributeDescriptor == null && !attribute.isNamespaceDeclaration()) {
        final String name = attribute.getName();
        if (!XmlUtil.attributeFromTemplateFramework(name, tag) && (!toolState.isCustomValuesEnabled() || !toolState.containsEntity(name))) {
          boolean maySwitchToHtml5 = HtmlUtil.isCustomHtml5Attribute(name) && !HtmlUtil.hasNonHtml5Doctype(tag);
          LocalQuickFix[] quickfixes = new LocalQuickFix[maySwitchToHtml5 ? 3 : 2];
          quickfixes[0] = new AddCustomHtmlElementIntentionAction(XmlEntitiesInspection.ATTRIBUTE_SHORT_NAME,
                                                                  name,
                                                                  XmlLocalize.addCustomHtmlAttribute(name).get()
          );
          quickfixes[1] = new RemoveAttributeIntentionAction(name);
          if (maySwitchToHtml5) {
            quickfixes[2] = new SwitchToHtml5WithHighPriorityAction();
          }

          registerProblemOnAttributeName(
            attribute,
            XmlErrorLocalize.attributeIsNotAllowedHere(attribute.getName()).get(),
            holder,
            quickfixes
          );
        }
      }
    }
  }
}
