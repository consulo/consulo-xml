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
package consulo.xml.lang.base;

import com.intellij.xml.util.XmlUtil;
import consulo.language.editor.surroundWith.SurroundDescriptor;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Pair;
import consulo.xml.psi.xml.XmlTagChild;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ven
 */
public abstract class XmlBasedSurroundDescriptor implements SurroundDescriptor {
  @Nonnull
  public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
    final Pair<XmlTagChild, XmlTagChild> childrenInRange = XmlUtil.findTagChildrenInRange(file, startOffset, endOffset);
    if (childrenInRange == null) {
      final PsiElement elementAt = file.findElementAt(startOffset);
      if (elementAt instanceof XmlToken &&
          ((XmlToken)elementAt).getTokenType() == XmlTokenType.XML_DATA_CHARACTERS) {
        return new PsiElement[] {elementAt};
      }
      return PsiElement.EMPTY_ARRAY;
    }
    List<PsiElement> result = new ArrayList<PsiElement>();
    PsiElement first = childrenInRange.getFirst();
    PsiElement last = childrenInRange.getSecond();
    while(true) {
      result.add(first);
      if (first == last) break;
      first = first.getNextSibling();
    }

    return result.toArray(PsiElement.ARRAY_FACTORY);
  }

  @Nonnull
  public Surrounder[] getSurrounders() {
    return new Surrounder[0]; //everything is in live templates now
  }

  @Override
  public boolean isExclusive() {
    return false;
  }
}
