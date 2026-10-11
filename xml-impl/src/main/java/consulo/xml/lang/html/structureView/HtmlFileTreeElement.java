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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationPropertiesComponent;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.ide.impl.idea.ide.util.treeView.smartTree.TreeStructureUtil;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.language.editor.structureView.StructureViewFactoryEx;
import consulo.language.psi.resolve.FilterElementProcessor;
import consulo.localize.LocalizeValue;
import consulo.xml.language.psi.XmlDocument;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.psi.filters.XmlTagFilter;

import java.util.*;

class HtmlFileTreeElement extends PsiTreeElementBase<XmlFile> {
    private final boolean myInStructureViewPopup;

    public HtmlFileTreeElement(boolean inStructureViewPopup, XmlFile xmlFile) {
        super(xmlFile);
        myInStructureViewPopup = inStructureViewPopup;
    }

    @Override
    @RequiredReadAction
    public Collection<StructureViewTreeElement> getChildrenBase() {
        if (isHtml5SectionsMode()) {
            return Collections.emptyList(); // Html5SectionsNodeProvider will return its structure
        }

        XmlFile xmlFile = getElement();
        XmlDocument document = xmlFile == null ? null : xmlFile.getDocument();
        if (document == null) {
            return Collections.emptyList();
        }

        List<XmlTag> rootTags = new ArrayList<>();
        document.processElements(new FilterElementProcessor(XmlTagFilter.INSTANCE, rootTags), document);

        if (rootTags.isEmpty()) {
            return Collections.emptyList();
        }

        if (rootTags.size() == 1) {
            XmlTag rootTag = rootTags.get(0);

            if ("html".equalsIgnoreCase(rootTag.getLocalName())) {
                XmlTag[] subTags = rootTag.getSubTags();
                if (subTags.length == 1 &&
                    ("head".equalsIgnoreCase(subTags[0].getLocalName()) || "body".equalsIgnoreCase(subTags[0].getLocalName()))) {
                    return new HtmlTagTreeElement(subTags[0]).getChildrenBase();
                }

                return new HtmlTagTreeElement(rootTag).getChildrenBase();
            }

            return Arrays.<StructureViewTreeElement>asList(new HtmlTagTreeElement(rootTag));
        }
        else {
            Collection<StructureViewTreeElement> result = new ArrayList<>();

            for (XmlTag tag : rootTags) {
                result.add(new HtmlTagTreeElement(tag));
            }

            return result;
        }
    }

    @RequiredReadAction
    private boolean isHtml5SectionsMode() {
        XmlFile xmlFile = getElement();
        if (xmlFile == null) {
            return false;
        }

        if (myInStructureViewPopup) {
            String propertyName = TreeStructureUtil.getPropertyName(Html5SectionsNodeProvider.HTML5_OUTLINE_PROVIDER_PROPERTY);
            if (ApplicationPropertiesComponent.getInstance().getBoolean(propertyName, false)) {
                return true;
            }
        }
        else if (StructureViewFactoryEx.getInstanceEx(xmlFile.getProject()).isActionActive(Html5SectionsNodeProvider.ACTION_ID)) {
            return true;
        }

        return false;
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        return LocalizeValue.of(toString()); // root element is not visible
    }
}
