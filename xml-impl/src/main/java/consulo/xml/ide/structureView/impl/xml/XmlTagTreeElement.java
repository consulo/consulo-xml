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

import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.structureView.StructureViewTreeElement;

import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlTag;
import org.jspecify.annotations.Nullable;

public class XmlTagTreeElement extends AbstractXmlTagTreeElement<XmlTag> {
    private static final String ID_ATTR_NAME = "id";
    private static final String NAME_ATTR_NAME = "name";

    public XmlTagTreeElement(XmlTag tag) {
        super(tag);
    }

    public Collection<StructureViewTreeElement> getChildrenBase() {
        return getStructureViewTreeElements(getElement().getSubTags());
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        XmlTag element = getElement();
        if (element == null) {
            return LocalizeValue.localizeTODO("*invalid*");
        }
        String id = element.getAttributeValue(ID_ATTR_NAME);
        if (id == null) {
            id = element.getAttributeValue(NAME_ATTR_NAME);
        }
        id = toCanonicalForm(id);

        if (id != null) {
            return LocalizeValue.localizeTODO(id + ":" + element.getLocalName());
        }
        return LocalizeValue.of(element.getName());
    }

    @Override
    @RequiredReadAction
    public String getLocationString() {
        StringBuilder buffer = new StringBuilder();
        XmlTag element = getElement();
        XmlAttribute[] attributes = element.getAttributes();

        String id = element.getAttributeValue(ID_ATTR_NAME);
        String usedAttrName = null;

        if (id == null) {
            id = element.getAttributeValue(NAME_ATTR_NAME);
            if (id != null) {
                usedAttrName = NAME_ATTR_NAME;
            }
        }
        else {
            usedAttrName = ID_ATTR_NAME;
        }

        id = toCanonicalForm(id);

        for (XmlAttribute attribute : attributes) {
            if (buffer.length() != 0) {
                buffer.append(" ");
            }

            String name = attribute.getName();
            if (usedAttrName != null && id != null && usedAttrName.equals(name)) {
                continue; // we output this name in name
            }

            buffer.append(name);
            buffer.append("=");
            buffer.append("\"");
            buffer.append(attribute.getValue());
            buffer.append("\"");
        }
        return buffer.toString();
    }

    @Nullable
    public static String toCanonicalForm(@Nullable String id) {
        return StringUtil.trimToNull(id);
    }
}
