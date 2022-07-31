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
package consulo.xml.codeInsight.navigation;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.ide.impl.idea.codeInsight.navigation.MethodNavigationOffsetProvider;
import consulo.ide.impl.idea.codeInsight.navigation.MethodUpDownUtil;
import consulo.language.psi.util.PsiTreeUtil;

import java.util.ArrayList;

/**
 * @author yole
 */
@ExtensionImpl(order = "last")
public class XmlMethodNavigationOffsetProvider implements MethodNavigationOffsetProvider {
  @Override
  public int[] getMethodNavigationOffsets(final PsiFile file, final int caretOffset) {
    if (file instanceof XmlFile) {
      PsiElement element = file;
      PsiElement elementAt = file.findElementAt(caretOffset);
      elementAt = PsiTreeUtil.getParentOfType(elementAt, XmlTag.class);
      if (elementAt != null) element = elementAt;

      ArrayList<PsiElement> array = new ArrayList<PsiElement>();
      addNavigationElements(array, element);
      return MethodUpDownUtil.offsetsFromElements(array);
    }
    return null;
  }

  private static void addNavigationElements(ArrayList<PsiElement> array, PsiElement element) {
    PsiElement parent = element instanceof XmlFile ? element : element.getParent();

    if (parent != null) {
      PsiElement[] children = parent.getChildren();
      for (PsiElement child : children) {
        if (child instanceof XmlTag) {
          array.add(child);
        }
      }
    }
    final PsiElement parentElement = element.getParent();
    if (parentElement != null) {
      addNavigationElements(array, parentElement);
    }
  }
}
