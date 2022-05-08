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

import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.xml.XmlFile;
import consulo.language.Language;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Eugene.Kudelevsky
 * Date: 3/29/11
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HtmlGotoRelatedProvider extends GotoRelatedProvider {
  @Nonnull
  @Override
  public List<? extends GotoRelatedItem> getItems(@Nonnull PsiElement context) {
    final PsiFile file = context.getContainingFile();
    if (file == null || !isAvailable(file)) {
      return Collections.emptyList();
    }

    return getRelatedFiles(file);
  }

  private static boolean isAvailable(@Nonnull PsiFile psiFile) {
    for (PsiFile file : psiFile.getViewProvider().getAllFiles()) {
      Language language = file.getLanguage();
      if (language.isKindOf(HTMLLanguage.INSTANCE) || language.isKindOf(XHTMLLanguage.INSTANCE)) {
        return true;
      }
    }
    return false;
  }

  private static List<? extends GotoRelatedItem> getRelatedFiles(@Nonnull PsiFile file) {
    List<GotoRelatedItem> items = new ArrayList<GotoRelatedItem>();

    for (PsiFile psiFile : file.getViewProvider().getAllFiles()) {
      if (psiFile instanceof XmlFile) {
        final XmlFile xmlFile = (XmlFile)psiFile;

        for (RelatedToHtmlFilesContributor contributor : RelatedToHtmlFilesContributor.EP_NAME.getExtensionList()) {
          Set<PsiFile> resultSet = new HashSet<PsiFile>();
          contributor.fillRelatedFiles(xmlFile, resultSet);
          for (PsiFile f: resultSet) {
            items.add(new GotoRelatedItem(f, contributor.getGroupName()));
          }
        }
      }
    }
    return items;
  }
}
