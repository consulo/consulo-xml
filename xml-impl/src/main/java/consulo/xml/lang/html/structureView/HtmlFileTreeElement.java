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

import consulo.application.ApplicationPropertiesComponent;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.ide.impl.idea.ide.util.treeView.smartTree.TreeStructureUtil;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.language.editor.structureView.StructureViewFactoryEx;
import consulo.language.psi.resolve.FilterElementProcessor;
import consulo.xml.psi.filters.XmlTagFilter;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

class HtmlFileTreeElement extends PsiTreeElementBase<XmlFile> {

  private final boolean myInStructureViewPopup;

  public HtmlFileTreeElement(final boolean inStructureViewPopup, final XmlFile xmlFile) {
    super(xmlFile);
    myInStructureViewPopup = inStructureViewPopup;
  }

  @Nonnull
  public Collection<StructureViewTreeElement> getChildrenBase() {
    if (isHtml5SectionsMode()) {
      return Collections.emptyList(); // Html5SectionsNodeProvider will return its structure
    }

    final XmlFile xmlFile = getElement();
    final XmlDocument document = xmlFile == null ? null : xmlFile.getDocument();
    if (document == null) return Collections.emptyList();

    final List<XmlTag> rootTags = new ArrayList<XmlTag>();
    document.processElements(new FilterElementProcessor(XmlTagFilter.INSTANCE, rootTags), document);

    if (rootTags.isEmpty()) return Collections.emptyList();

    if (rootTags.size() == 1) {
      final XmlTag rootTag = rootTags.get(0);

      if ("html".equalsIgnoreCase(rootTag.getLocalName())) {
        final XmlTag[] subTags = rootTag.getSubTags();
        if (subTags.length == 1 &&
            ("head".equalsIgnoreCase(subTags[0].getLocalName()) || "body".equalsIgnoreCase(subTags[0].getLocalName()))) {
          return new HtmlTagTreeElement(subTags[0]).getChildrenBase();
        }

        return new HtmlTagTreeElement(rootTag).getChildrenBase();
      }

      return Arrays.<StructureViewTreeElement>asList(new HtmlTagTreeElement(rootTag));
    }
    else {
      final Collection<StructureViewTreeElement> result = new ArrayList<StructureViewTreeElement>();

      for (XmlTag tag : rootTags) {
        result.add(new HtmlTagTreeElement(tag));
      }

      return result;
    }
  }

  private boolean isHtml5SectionsMode() {
    final XmlFile xmlFile = getElement();
    if (xmlFile == null) return false;

    if (myInStructureViewPopup) {
      final String propertyName = TreeStructureUtil.getPropertyName(Html5SectionsNodeProvider.HTML5_OUTLINE_PROVIDER_PROPERTY);
      if (ApplicationPropertiesComponent.getInstance().getBoolean(propertyName, false)) {
        return true;
      }
    }
    else if (StructureViewFactoryEx.getInstanceEx(xmlFile.getProject()).isActionActive(Html5SectionsNodeProvider.ACTION_ID)) {
      return true;
    }

    return false;
  }

  @Nullable
  public String getPresentableText() {
    return toString(); // root element is not visible
  }
}
