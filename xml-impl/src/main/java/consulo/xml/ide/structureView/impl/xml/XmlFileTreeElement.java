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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.xml.language.psi.*;

public class XmlFileTreeElement extends AbstractXmlTagTreeElement<XmlFile> {
    public XmlFileTreeElement(XmlFile file) {
        super(file);
    }

    @Override
    @RequiredReadAction
    public Collection<StructureViewTreeElement> getChildrenBase() {
        XmlDocument document = getElement().getDocument();
        List<XmlTag> rootTags = new ArrayList<>();
        if (document != null) {
            for (PsiElement element : document.getChildren()) {
                if (element instanceof XmlTag tag) {
                    rootTags.add(tag);
                }
            }
        }

        Collection<StructureViewTreeElement> structureViewTreeElements =
            getStructureViewTreeElements(rootTags.toArray(new XmlTag[rootTags.size()]));

        Collection<StructureViewTreeElement> dtdStructureViewTreeElements = null;
        XmlProlog prolog = document != null ? document.getProlog() : null;
        if (prolog != null) {
            XmlDoctype doctype = prolog.getDoctype();

            if (doctype != null) {
                XmlMarkupDecl xmlMarkupDecl = doctype.getMarkupDecl();
                if (xmlMarkupDecl != null) {
                    dtdStructureViewTreeElements = DtdFileTreeElement.collectElements(xmlMarkupDecl);
                }
            }
        }

        if (dtdStructureViewTreeElements != null) {
            ArrayList<StructureViewTreeElement> result =
                new ArrayList<>(dtdStructureViewTreeElements.size() + structureViewTreeElements.size());

            result.addAll(dtdStructureViewTreeElements);
            result.addAll(structureViewTreeElements);
            structureViewTreeElements = result;
        }
        return structureViewTreeElements;
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        return LocalizeValue.of(getElement().getName());
    }
}
