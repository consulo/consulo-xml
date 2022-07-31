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
package consulo.xml.ide.structureView.impl.xml;

import java.util.Collection;

import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.xml.ide.structureView.xml.XmlStructureViewElementProvider;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.ide.impl.idea.util.Function;
import consulo.util.collection.ContainerUtil;

public abstract class AbstractXmlTagTreeElement<T extends XmlElement> extends PsiTreeElementBase<T> {
  protected AbstractXmlTagTreeElement(final T psiElement) {
    super(psiElement);
  }

  protected static Collection<StructureViewTreeElement> getStructureViewTreeElements(XmlTag[] subTags) {
    final XmlStructureViewElementProvider[] providers = XmlStructureViewElementProvider.EP_NAME.getExtensions();

    return ContainerUtil.map2List(subTags, new Function<XmlTag, StructureViewTreeElement>() {
      public StructureViewTreeElement fun(final XmlTag xmlTag) {
        for (final XmlStructureViewElementProvider provider : providers) {
          final StructureViewTreeElement element = provider.createCustomXmlTagTreeElement(xmlTag);
          if (element != null) {
            return element;
          }
        }
        return new XmlTagTreeElement(xmlTag);
      }
    });
  }
}