/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.xml.impl.schema;

import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;

import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class XmlElementsGroupLeaf extends XmlElementsGroupBase {
    private final XmlElementDescriptor myDescriptor;

    public XmlElementsGroupLeaf(XmlTag tag, XmlElementDescriptor descriptor, XmlElementsGroup parent, XmlTag ref) {
        super(tag, parent, ref);
        myDescriptor = descriptor;
    }

    @Override
    public Type getGroupType() {
        return Type.LEAF;
    }

    @Override
    public List<XmlElementsGroup> getSubGroups() {
        return Collections.emptyList();
    }

    @Override
    public XmlElementDescriptor getLeafDescriptor() {
        return myDescriptor;
    }
}
