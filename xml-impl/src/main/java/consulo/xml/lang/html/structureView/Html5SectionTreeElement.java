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
package consulo.xml.lang.html.structureView;

import consulo.application.util.function.Computable;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.editor.structureView.PsiTreeElementBase;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

class Html5SectionTreeElement extends PsiTreeElementBase<XmlTag> {

  private final Computable<Collection<StructureViewTreeElement>> myChildrenComputable;
  private final String myHeader;

  public Html5SectionTreeElement(final XmlTag tag,
                                 final Computable<Collection<StructureViewTreeElement>> childrenComputable,
                                 final @Nullable String header) {
    super(tag);
    myChildrenComputable = childrenComputable;
    myHeader = header;
  }

  @Nonnull
  public Collection<StructureViewTreeElement> getChildrenBase() {
    return myChildrenComputable.compute();
  }

  public String getPresentableText() {
    if (myHeader != null) {
      return HtmlTagTreeElement.normalizeSpacesAndShortenIfLong(myHeader);
    }

    final XmlTag tag = getElement();
    return tag == null ? null : HtmlTagTreeElement.normalizeSpacesAndShortenIfLong(tag.getValue().getTrimmedText());
  }

  public String getLocationString() {
    final XmlTag tag = getElement();
    if (tag == null) return null;

    return HtmlTagTreeElement.getTagPresentation(tag);
  }

  public boolean isSearchInLocationString() {
    return true;
  }
}
