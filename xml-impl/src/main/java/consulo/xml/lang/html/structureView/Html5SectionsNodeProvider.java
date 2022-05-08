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

import consulo.xml.psi.filters.XmlTagFilter;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlBundle;
import consulo.application.AllIcons;
import consulo.fileEditor.structureView.tree.ActionPresentation;
import consulo.fileEditor.structureView.tree.ActionPresentationData;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.ide.impl.idea.ide.util.FileStructureNodeProvider;
import consulo.ide.impl.idea.openapi.util.PropertyOwner;
import consulo.language.psi.resolve.FilterElementProcessor;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.keymap.KeymapManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Html5SectionsNodeProvider implements FileStructureNodeProvider<Html5SectionTreeElement>, PropertyOwner {

  public static final String ACTION_ID = "HTML5_OUTLINE_MODE";
  public static final String HTML5_OUTLINE_PROVIDER_PROPERTY = "html5.sections.node.provider";

  @Nonnull
  public String getName() {
    return ACTION_ID;
  }

  @Nonnull
  public ActionPresentation getPresentation() {
    return new ActionPresentationData(XmlBundle.message("html5.outline.mode"), null, AllIcons.Xml.Html5);
  }

  public String getCheckBoxText() {
    return XmlBundle.message("html5.outline.mode");
  }

  public Shortcut[] getShortcut() {
    return KeymapManager.getInstance().getActiveKeymap().getShortcuts("FileStructurePopup");
  }

  @Nonnull
  public String getPropertyName() {
    return HTML5_OUTLINE_PROVIDER_PROPERTY;
  }

  public Collection<Html5SectionTreeElement> provideNodes(final TreeElement node) {
    if (!(node instanceof HtmlFileTreeElement)) return Collections.emptyList();

    final XmlFile xmlFile = ((HtmlFileTreeElement)node).getElement();
    final XmlDocument document = xmlFile == null ? null : xmlFile.getDocument();
    if (document == null) return Collections.emptyList();

    final List<XmlTag> rootTags = new ArrayList<XmlTag>();
    document.processElements(new FilterElementProcessor(XmlTagFilter.INSTANCE, rootTags), document);

    final Collection<Html5SectionTreeElement> result = new ArrayList<Html5SectionTreeElement>();

    for (XmlTag tag : rootTags) {
      result.addAll(Html5SectionsProcessor.processAndGetRootSections(tag));
    }

    return result;
  }
}
