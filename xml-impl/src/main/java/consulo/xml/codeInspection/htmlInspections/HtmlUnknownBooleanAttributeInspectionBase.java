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
import com.intellij.xml.XmlBundle;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.XmlEnumerationDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.util.HtmlUtil;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInspection.XmlQuickFixFactory;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

public abstract class HtmlUnknownBooleanAttributeInspectionBase extends HtmlUnknownElementInspection {
  @Override
  @Nonnull
  public String getDisplayName() {
    return XmlLocalize.htmlInspectionsUnknownBooleanAttribute().get();
  }

  @Override
  @NonNls
  @Nonnull
  public String getShortName() {
    return XmlEntitiesInspection.BOOLEAN_ATTRIBUTE_SHORT_NAME;
  }

  @Override
  protected LocalizeValue getCheckboxTitle() {
    return XmlLocalize.htmlInspectionsUnknownTagBooleanAttributeCheckboxTitle();
  }

  @Override
  protected void checkAttribute(@Nonnull final XmlAttribute attribute,
                                @Nonnull final ProblemsHolder holder,
                                final boolean isOnTheFly,
                                Object state) {
    if (attribute.getValueElement() == null) {
      final XmlTag tag = attribute.getParent();

      if (tag instanceof HtmlTag) {
        XmlElementDescriptor elementDescriptor = tag.getDescriptor();
        if (elementDescriptor == null || elementDescriptor instanceof AnyXmlElementDescriptor) {
          return;
        }

        BaseHtmlEntitiesInspectionState toolState = (BaseHtmlEntitiesInspectionState)state;

        XmlAttributeDescriptor attributeDescriptor = elementDescriptor.getAttributeDescriptor(attribute);
        if (attributeDescriptor != null && !(attributeDescriptor instanceof AnyXmlAttributeDescriptor)) {
          String name = attribute.getName();
          if (!HtmlUtil.isBooleanAttribute(attributeDescriptor, null) && (!toolState.isCustomValuesEnabled() || !toolState.containsEntity(name))) {
            final boolean html5 = HtmlUtil.isHtml5Context(tag);
            LocalQuickFix[] quickFixes = !html5 ? new LocalQuickFix[]{
              new AddCustomHtmlElementIntentionAction(XmlEntitiesInspection.BOOLEAN_ATTRIBUTE_SHORT_NAME,
                                                      name,
                                                      XmlBundle.message("add.custom.html.boolean.attribute", name)),
              XmlQuickFixFactory.getInstance().addAttributeValueFix(attribute),
              new RemoveAttributeIntentionAction(name),
            } : new LocalQuickFix[]{
              XmlQuickFixFactory.getInstance().addAttributeValueFix(attribute)
            };


            LocalizeValue error = null;
            if (html5) {
              if (attributeDescriptor instanceof XmlEnumerationDescriptor enumDesc
                && enumDesc.getValueDeclaration(attribute, "") == null) {
                error = XmlErrorLocalize.wrongValue("attribute");
              }
            }
            else {
              error = XmlErrorLocalize.attributeIsNotBoolean(attribute.getName());
            }
            if (error != null) {
              registerProblemOnAttributeName(attribute, error.get(), holder, quickFixes);
            }
          }
        }
      }
    }
  }
}
