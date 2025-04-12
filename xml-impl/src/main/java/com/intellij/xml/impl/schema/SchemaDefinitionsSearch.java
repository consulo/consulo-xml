/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.xml.index.SchemaTypeInfo;
import com.intellij.xml.index.SchemaTypeInheritanceIndex;
import com.intellij.xml.index.XmlNamespaceIndex;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.search.DefinitionsScopedSearch;
import consulo.language.psi.search.DefinitionsScopedSearchExecutor;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.impl.source.xml.XmlTagImpl;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Irina.Chernushina
 * @since 2021-07-05
 */
@ExtensionImpl
public class SchemaDefinitionsSearch implements DefinitionsScopedSearchExecutor {
    @Override
    @RequiredReadAction
    public boolean execute(
        @Nonnull DefinitionsScopedSearch.SearchParameters parameters,
        @Nonnull Predicate<? super PsiElement> consumer
    ) {
        PsiElement queryParameters = parameters.getElement();
        if (queryParameters instanceof XmlTagImpl xml && isTypeElement(xml)) {
            Application app = Application.get();
            Collection<SchemaTypeInfo> infos =
                app.runReadAction((Supplier<Collection<SchemaTypeInfo>>)() -> gatherInheritors(xml));

            if (infos != null && !infos.isEmpty()) {
                XmlFile file = XmlUtil.getContainingFile(xml);
                Project project = file.getProject();
                Module module = queryParameters.getModule();
                //if (module == null) return false;

                VirtualFile vf = file.getVirtualFile();
                String thisNs = XmlNamespaceIndex.getNamespace(vf, project, file);
                thisNs = thisNs == null ? getDefaultNs(file) : thisNs;
                // so thisNs can be null
                if (thisNs == null) {
                    return false;
                }
                List<SchemaTypeInfo> infosLst = new ArrayList<>(infos);
                Collections.sort(infosLst);
                Map<String, Set<XmlFile>> nsMap = new HashMap<>();
                for (SchemaTypeInfo info : infosLst) {
                    Set<XmlFile> targetFiles = nsMap.get(info.getNamespaceUri());
                    if (targetFiles == null) {
                        targetFiles = new HashSet<>();
                        if (Comparing.equal(info.getNamespaceUri(), thisNs)) {
                            targetFiles.add(file);
                        }
                        Collection<XmlFile> files = app.runReadAction(
                            (Supplier<Collection<XmlFile>>)() -> XmlUtil.findNSFilesByURI(info.getNamespaceUri(), project, module)
                        );
                        if (files != null) {
                            targetFiles.addAll(files);
                        }
                        nsMap.put(info.getNamespaceUri(), targetFiles);
                    }
                    if (!targetFiles.isEmpty()) {
                        for (XmlFile targetFile : targetFiles) {
                            app.runReadAction(() -> {
                                String prefixByURI = XmlUtil.findNamespacePrefixByURI(targetFile, info.getNamespaceUri());
                                if (prefixByURI == null) {
                                    return;
                                }
                                PsiElementProcessor processor = element -> {
                                    if (element instanceof XmlTagImpl tag) {
                                        if (isCertainTypeElement(tag, info.getTagName(), prefixByURI)
                                            || isElementWithEmbeddedType(tag, info.getTagName(), prefixByURI)) {
                                            consumer.test(element);
                                            return false;
                                        }
                                    }
                                    return true;
                                };
                                XmlUtil.processXmlElements(targetFile, processor, true);
                            });
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean isElementWithSomeEmbeddedType(XmlTagImpl xml) {
        String localName = xml.getLocalName();
        if (!(XmlUtil.XML_SCHEMA_URI.equals(xml.getNamespace()) && "element".equals(localName))) {
            return false;
        }
        XmlTag[] tags = xml.getSubTags();
        for (XmlTag tag : tags) {
            if (isTypeElement((XmlTagImpl)tag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isElementWithEmbeddedType(XmlTagImpl xml, String typeName, String typeNsPrefix) {
        String localName = xml.getLocalName();
        if (!(XmlUtil.XML_SCHEMA_URI.equals(xml.getNamespace()) && "element".equals(localName))) {
            return false;
        }
        XmlAttribute nameAttr = getNameAttr(xml);
        if (nameAttr == null || nameAttr.getValue() == null) {
            return false;
        }
        String localTypeName = XmlUtil.findLocalNameByQualifiedName(nameAttr.getValue());
        String prefix = XmlUtil.findPrefixByQualifiedName(nameAttr.getValue());
        if (!typeName.equals(localTypeName) || !typeNsPrefix.equals(prefix)) {
            return false;
        }
        XmlTag[] tags = xml.getSubTags();
        for (XmlTag tag : tags) {
            if (isTypeElement((XmlTagImpl)tag)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCertainTypeElement(XmlTagImpl xml, String typeName, String nsPrefix) {
        if (!isTypeElement(xml)) {
            return false;
        }
        XmlAttribute name = getNameAttr(xml);
        if (name == null) {
            return false;
        }
        String value = name.getValue();
        if (value == null) {
            return false;
        }
        String localName = XmlUtil.findLocalNameByQualifiedName(value);
        return typeName.equals(localName) && nsPrefix.equals(XmlUtil.findPrefixByQualifiedName(value));
    }

    public static boolean isTypeElement(XmlTagImpl xml) {
        String localName = xml.getLocalName();
        return XmlUtil.XML_SCHEMA_URI.equals(xml.getNamespace()) && ("complexType".equals(localName) || "simpleType".equals(localName));
    }

    private Collection<SchemaTypeInfo> gatherInheritors(XmlTagImpl xml) {
        XmlAttribute name = getNameAttr(xml);
        if (name == null || StringUtil.isEmptyOrSpaces(name.getValue())) {
            return null;
        }
        String localName = name.getValue();
        boolean hasPrefix = localName.contains(":");
        localName = hasPrefix ? localName.substring(localName.indexOf(':') + 1) : localName;
        String nsPrefix = hasPrefix ? name.getValue().substring(0, name.getValue().indexOf(':')) : null;

        XmlFile file = XmlUtil.getContainingFile(xml);
        if (file == null) {
            return null;
        }
        Project project = file.getProject();
        if (project == null) {
            return null;
        }

        Set<SchemaTypeInfo> result = new HashSet<>();
        ArrayDeque<SchemaTypeInfo> queue = new ArrayDeque<>();

        String nsUri;
        if (!hasPrefix) {
            nsUri = getDefaultNs(file);
        }
        else {
            nsUri = XmlUtil.findNamespaceByPrefix(nsPrefix, file.getRootTag());
        }
        if (nsUri == null) {
            return null;
        }

        queue.add(new SchemaTypeInfo(localName, true, nsUri));

        BiFunction<String, String, List<Set<SchemaTypeInfo>>> worker =
            SchemaTypeInheritanceIndex.getWorker(project, file.getContainingFile().getVirtualFile());
        while (!queue.isEmpty()) {
            SchemaTypeInfo info = queue.removeFirst();
            List<Set<SchemaTypeInfo>> childrenOfType = worker.apply(info.getNamespaceUri(), info.getTagName());
            for (Set<SchemaTypeInfo> infos : childrenOfType) {
                for (SchemaTypeInfo typeInfo : infos) {
                    if (typeInfo.isIsTypeName()) {
                        queue.add(typeInfo);
                    }
                    result.add(typeInfo);
                }
            }
        }
        return result;
    }

    public static XmlAttribute getNameAttr(XmlTagImpl xml) {
        XmlAttribute name = xml.getAttribute("name", XmlUtil.XML_SCHEMA_URI);
        name = name == null ? xml.getAttribute("name") : name;
        return name;
    }

    private String getDefaultNs(XmlFile file) {
        return Application.get().runReadAction((Supplier<String>)() -> {
            String nsUri;
            XmlTag tag = file.getDocument().getRootTag();
            XmlAttribute xmlns = tag.getAttribute("xmlns", XmlUtil.XML_SCHEMA_URI);
            xmlns = xmlns == null ? tag.getAttribute("xmlns") : xmlns;
            nsUri = xmlns == null ? null : xmlns.getValue();
            return nsUri;
        });
    }
}
