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

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.OuterLanguageElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiWhiteSpace;
import consulo.xml.codeInspection.XmlInspectionGroupNames;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
public abstract class HtmlLocalInspectionTool extends XmlSuppressableInspectionTool {

  @Override
  @Nls
  @Nonnull
  public String getGroupDisplayName() {
    return XmlInspectionGroupNames.HTML_INSPECTIONS;
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  protected void checkTag(@Nonnull final XmlTag tag, @Nonnull final ProblemsHolder holder, final boolean isOnTheFly, Object state) {
    // should be overridden
  }

  protected void checkAttribute(@Nonnull final XmlAttribute attribute,
                                @Nonnull final ProblemsHolder holder,
                                final boolean isOnTheFly,
                                Object state) {
    // should be overridden
  }

  @Nonnull
  @Override
  public PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder,
                                        boolean isOnTheFly,
                                        @Nonnull LocalInspectionToolSession session,
                                        @Nonnull Object state) {
    return new XmlElementVisitor() {
      @Override
      @RequiredReadAction
      public void visitXmlToken(final XmlToken token) {
        if (token.getTokenType() == XmlTokenType.XML_NAME) {
          PsiElement element = token.getPrevSibling();
          while (element instanceof PsiWhiteSpace) element = element.getPrevSibling();

          if (element instanceof XmlToken && ((XmlToken)element).getTokenType() == XmlTokenType.XML_START_TAG_START) {
            PsiElement parent = element.getParent();

            if (parent instanceof XmlTag && !(token.getNextSibling() instanceof OuterLanguageElement)) {
              XmlTag tag = (XmlTag)parent;
              checkTag(tag, holder, isOnTheFly, state);
            }
          }
        }
      }

      @Override
      public void visitXmlAttribute(final XmlAttribute attribute) {
        checkAttribute(attribute, holder, isOnTheFly, state);
      }
    };
  }
}
