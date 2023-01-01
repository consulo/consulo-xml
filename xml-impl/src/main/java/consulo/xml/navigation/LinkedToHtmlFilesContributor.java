/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.xml.navigation;

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.application.util.function.Processor;
import com.intellij.xml.util.HtmlLinkUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ResolveResult;

import javax.annotation.Nonnull;

import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class LinkedToHtmlFilesContributor extends RelatedToHtmlFilesContributor {
  @Override
  public void fillRelatedFiles(@Nonnull final XmlFile xmlFile, @Nonnull final Set<PsiFile> resultSet) {
    HtmlLinkUtil.processLinks(xmlFile, new Processor<XmlTag>() {
      @Override
      public boolean process(XmlTag tag) {
        final XmlAttribute attribute = tag.getAttribute("href");
        if (attribute == null) {
          return true;
        }

        final XmlAttributeValue link = attribute.getValueElement();
        if (link == null) {
          return true;
        }

        for (PsiReference reference : link.getReferences()) {
          if (reference instanceof PsiPolyVariantReference) {
            final ResolveResult[] results = ((PsiPolyVariantReference)reference).multiResolve(false);

            for (ResolveResult result : results) {
              final PsiElement resolvedElement = result.getElement();
              if (resolvedElement instanceof PsiFile) {
                resultSet.add((PsiFile)resolvedElement);
              }
            }
          }
          else {
            final PsiElement resolvedElement = reference.resolve();
            if (resolvedElement instanceof PsiFile) {
              resultSet.add((PsiFile)resolvedElement);
            }
          }
        }
        return true;
      }
    });
  }

  @Override
  public String getGroupName() {
    return "Linked files";
  }
}
