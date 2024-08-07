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
package com.intellij.html.impl;

import com.intellij.html.index.Html5CustomAttributesIndex;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlAttributeDescriptorsProvider;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import com.intellij.xml.util.HtmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class Html5CustomAttributeDescriptorsProvider implements XmlAttributeDescriptorsProvider {
    @Override
    public XmlAttributeDescriptor[] getAttributeDescriptors(XmlTag tag) {
        if (tag == null || !HtmlUtil.isHtml5Context(tag)) {
            return XmlAttributeDescriptor.EMPTY;
        }
        final List<String> currentAttrs = new ArrayList<>();
        for (XmlAttribute attribute : tag.getAttributes()) {
            currentAttrs.add(attribute.getName());
        }
        final Project project = tag.getProject();
        final Collection<String> keys = CachedValuesManager.getManager(project).getCachedValue(project, () ->
        {
            final Collection<String> keys1 = FileBasedIndex.getInstance().getAllKeys(Html5CustomAttributesIndex.INDEX_ID, project);
            final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
            return CachedValueProvider.Result.<Collection<String>>create(ContainerUtil.filter(
                keys1,
                key -> !FileBasedIndex.getInstance().processValues(Html5CustomAttributesIndex.INDEX_ID, key,
                    null, (file, value) -> false, scope
                )
            ), PsiModificationTracker.MODIFICATION_COUNT);
        });
        if (keys.isEmpty()) {
            return XmlAttributeDescriptor.EMPTY;
        }

        final List<XmlAttributeDescriptor> result = new ArrayList<>();
        for (String key : keys) {
            boolean add = true;
            for (String attr : currentAttrs) {
                if (attr.startsWith(key)) {
                    add = false;
                }
            }
            if (add) {
                result.add(new AnyXmlAttributeDescriptor(key));
            }
        }

        return result.toArray(new XmlAttributeDescriptor[result.size()]);
    }

    @Override
    public XmlAttributeDescriptor getAttributeDescriptor(String attributeName, XmlTag context) {
        if (context != null && HtmlUtil.isCustomHtml5Attribute(attributeName) && HtmlUtil.tagHasHtml5Schema(context)) {
            return new AnyXmlAttributeDescriptor(attributeName);
        }
        return null;
    }

}
