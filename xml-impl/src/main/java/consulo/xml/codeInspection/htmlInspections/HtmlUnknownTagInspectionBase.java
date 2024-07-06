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

import com.intellij.xml.XmlBundle;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlTagUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.codeInspection.XmlQuickFixFactory;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.impl.localize.XmlLocalize;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.impl.source.html.dtd.HtmlElementDescriptorImpl;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class HtmlUnknownTagInspectionBase extends HtmlUnknownElementInspection {
  @Nonnull
  @Override
  public InspectionToolState<?> createStateProvider() {
    return new HtmlUnknownTagInspectionState(getCheckboxTitle());
  }

  private static boolean isAbstractDescriptor(XmlElementDescriptor descriptor) {
    return descriptor == null || descriptor instanceof AnyXmlElementDescriptor;
  }

  @Override
  @Nls
  @Nonnull
  public String getDisplayName() {
    return XmlLocalize.htmlInspectionsUnknownTag().get();
  }

  @Override
  @NonNls
  @Nonnull
  public String getShortName() {
    return XmlEntitiesInspection.TAG_SHORT_NAME;
  }

  @Override
  protected LocalizeValue getCheckboxTitle() {
    return LocalizeValue.localizeTODO(XmlLocalize.htmlInspectionsUnknownTagCheckboxTitle().get());
  }

  @Override
  protected void checkTag(@Nonnull final XmlTag tag, @Nonnull final ProblemsHolder holder, final boolean isOnTheFly, Object state) {
    if (!(tag instanceof HtmlTag) || !XmlHighlightVisitor.shouldBeValidated(tag)) {
      return;
    }

    BaseHtmlEntitiesInspectionState toolState = (BaseHtmlEntitiesInspectionState)state;

    XmlElementDescriptor descriptorFromContext = XmlUtil.getDescriptorFromContext(tag);

    PsiElement parent = tag.getParent();
    XmlElementDescriptor parentDescriptor = parent instanceof XmlTag ? ((XmlTag)parent).getDescriptor() : null;

    XmlElementDescriptor ownDescriptor = isAbstractDescriptor(descriptorFromContext) ? tag.getDescriptor() : descriptorFromContext;

    if (isAbstractDescriptor(ownDescriptor) || (parentDescriptor instanceof HtmlElementDescriptorImpl && ownDescriptor instanceof HtmlElementDescriptorImpl && isAbstractDescriptor
      (descriptorFromContext))) {

      final String name = tag.getName();

      if (!toolState.isCustomValuesEnabled() || !toolState.containsEntity(name)) {
        final AddCustomHtmlElementIntentionAction action = new AddCustomHtmlElementIntentionAction(XmlEntitiesInspection.TAG_SHORT_NAME,
                                                                                                   name,
                                                                                                   XmlBundle.message("add.custom.html.tag",
                                                                                                                     name));

        // todo: support "element is not allowed" message for html5
        // some tags in html5 cannot be found in xhtml5.xsd if they are located in incorrect context, so they get any-element descriptor (ex. "canvas: tag)
        final LocalizeValue message =
          isAbstractDescriptor(ownDescriptor) ? XmlErrorLocalize.unknownHtmlTag(name) : XmlErrorLocalize.elementIsNotAllowedHere(name);

        final PsiElement startTagName = XmlTagUtil.getStartTagNameElement(tag);
        assert startTagName != null;
        final PsiElement endTagName = XmlTagUtil.getEndTagNameElement(tag);

        List<LocalQuickFix> quickfixes = new ArrayList<>();
        quickfixes.add(action);
        if (isOnTheFly) {
          PsiFile file = startTagName.getContainingFile();
          if (file instanceof XmlFile) {
            quickfixes.add(XmlQuickFixFactory.getInstance().createNSDeclarationIntentionFix(startTagName, "", null));
          }

          // People using non-HTML as their template data language (but having not changed this in the IDE)
          // will most likely see 'unknown html tag' error, because HTML is usually the default.
          // So if they check quick fixes for this error they'll discover Change Template Data Language feature.
          ContainerUtil.addIfNotNull(quickfixes, createChangeTemplateDataFix(file));
        }
        if (HtmlUtil.isHtml5Tag(name) && !HtmlUtil.hasNonHtml5Doctype(tag)) {
          quickfixes.add(new SwitchToHtml5WithHighPriorityAction());
        }
        ProblemHighlightType highlightType =
          tag.getContainingFile().getContext() == null ? ProblemHighlightType.GENERIC_ERROR_OR_WARNING : ProblemHighlightType.INFORMATION;
        if (startTagName.getTextLength() > 0) {
          holder.registerProblem(startTagName, message.get(), highlightType, quickfixes.toArray(new LocalQuickFix[quickfixes.size()]));
        }

        if (endTagName != null) {
          holder.registerProblem(endTagName, message.get(), highlightType, quickfixes.toArray(new LocalQuickFix[quickfixes.size()]));
        }
      }
    }
  }

  @Nullable
  protected LocalQuickFix createChangeTemplateDataFix(PsiFile file) {
    return null;
  }
}
