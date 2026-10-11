/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.xml.impl.dtd.XmlElementDescriptorImpl;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.language.psi.PsiNamedElement;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.xml.language.psi.*;

import java.util.*;

public class DtdFileTreeElement extends PsiTreeElementBase<XmlFile> {
    public DtdFileTreeElement(XmlFile file) {
        super(file);
    }

    @Override
    @RequiredReadAction
    public Collection<StructureViewTreeElement> getChildrenBase() {
        return collectElements(getElement().getDocument());
    }

    static List<StructureViewTreeElement> collectElements(XmlElement element) {
        List<StructureViewTreeElement> elements = new ArrayList<>();

        XmlUtil.processXmlElements(
            element,
            thisElement -> {
                if (thisElement instanceof XmlElementDecl || thisElement instanceof XmlEntityDecl) {
                    elements.add(new DtdTreeElement((PsiNamedElement) thisElement));
                }
                return true;
            },
            false
        );
        return elements;
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        return LocalizeValue.of(getElement().getName());
    }

    private static class DtdTreeElement extends PsiTreeElementBase<PsiNamedElement> {
        private static final String IMPLIED = "implied";
        private static final String REQUIRED = "required";
        private static final String FIXED = "fixed";
        private static final String ID = "id";
        private static final String IDREF = "idref";
        private static final String ENUM = "enum";

        public DtdTreeElement(PsiNamedElement element) {
            super(element);
        }

        @Override
        public Collection<StructureViewTreeElement> getChildrenBase() {
            return Collections.emptyList();
        }

        @Override
        @RequiredReadAction
        public LocalizeValue getPresentableText() {
            PsiNamedElement namedElement = getElement();
            return namedElement != null ? LocalizeValue.ofNullable(namedElement.getName()) : LocalizeValue.empty();
        }

        @Override
        @RequiredReadAction
        public Image getIcon() {
            return PlatformIconGroup.nodesTag();
        }

        @Override
        @RequiredReadAction
        public String getLocationString() {
            XmlElement owner = (XmlElement) getElement();

            XmlAttlistDecl[] attLists = owner instanceof XmlElementDecl
                ? XmlElementDescriptorImpl.getCachedAttributeDeclarations(owner)
                : XmlAttlistDecl.EMPTY_ARRAY;

            if (attLists.length > 0) {
                Map<String, XmlAttributeDecl> attrMap = null;

                String name = getElement().getName();
                for (XmlAttlistDecl a : attLists) {
                    if (!Objects.equals(a.getName(), name)) {
                        continue;
                    }
                    if (attrMap == null) {
                        attrMap = new LinkedHashMap<>();
                    }

                    for (XmlAttributeDecl d : a.getAttributeDecls()) {
                        attrMap.put(d.getName(), d);
                    }
                }

                StringBuilder b = null;
                if (attrMap != null) {
                    for (Map.Entry<String, XmlAttributeDecl> e : attrMap.entrySet()) {
                        if (b == null) {
                            b = new StringBuilder();
                        }
                        else {
                            b.append(", ");
                        }
                        b.append(e.getKey());
                        XmlAttributeDecl attributeDecl = e.getValue();
                        String type = null;

                        if (attributeDecl.isIdAttribute()) {
                            type = ID;
                        }
                        else if (attributeDecl.isIdRefAttribute()) {
                            type = IDREF;
                        }
                        else if (attributeDecl.isEnumerated()) {
                            type = ENUM;
                        }

                        if (attributeDecl.isAttributeFixed()) {
                            if (type == null) {
                                type = FIXED;
                            }
                            else {
                                type += " " + FIXED;
                            }
                        }
                        else if (attributeDecl.isAttributeRequired()) {
                            if (type == null) {
                                type = REQUIRED;
                            }
                            else {
                                type += " " + REQUIRED;
                            }
                        }
                        else if (attributeDecl.isAttributeImplied()) {
                            if (type == null) {
                                type = IMPLIED;
                            }
                            else {
                                type += " " + IMPLIED;
                            }
                        }

                        if (type != null) {
                            b.append(':').append(type);
                        }
                        XmlAttributeValue value = attributeDecl.getDefaultValue();

                        if (value != null) {
                            b.append("=").append(value);
                        }
                    }
                }

                if (b != null) {
                    return b.toString();
                }
            }
            return super.getLocationString();
        }

        @Override
        @RequiredReadAction
        public String toString() {
            String s = getLocationString();
            String name = getElement().getName();
            if (StringUtil.isEmpty(s)) {
                return name;
            }
            return name + " (" + s + ")";
        }
    }
}