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
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.application.util.function.Processor;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.ide.impl.idea.util.PairConvertor;
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

import javax.annotation.Nonnull;
import java.util.*;

/**
 * User: Irina.Chernushina
 * Date: 7/5/12
 * Time: 5:34 PM
 */
@ExtensionImpl
public class SchemaDefinitionsSearch implements DefinitionsScopedSearchExecutor {
    @Override
    public boolean execute(
        @Nonnull final DefinitionsScopedSearch.SearchParameters parameters,
        @Nonnull final Processor<? super PsiElement> consumer
    ) {
        PsiElement queryParameters = parameters.getElement();
        if (queryParameters instanceof XmlTagImpl) {
            final XmlTagImpl xml = (XmlTagImpl)queryParameters;
            if (isTypeElement(xml)) {
                final Collection<SchemaTypeInfo> infos =
                    ApplicationManager.getApplication().runReadAction(new Computable<Collection<SchemaTypeInfo>>() {
                        @Override
                        public Collection<SchemaTypeInfo> compute() {
                            return gatherInheritors(xml);
                        }
                    });

                if (infos != null && !infos.isEmpty()) {
                    XmlFile file = XmlUtil.getContainingFile(xml);
                    final Project project = file.getProject();
                    final Module module = ModuleUtil.findModuleForPsiElement(queryParameters);
                    //if (module == null) return false;

                    final VirtualFile vf = file.getVirtualFile();
                    String thisNs = XmlNamespaceIndex.getNamespace(vf, project, file);
                    thisNs = thisNs == null ? getDefaultNs(file) : thisNs;
                    // so thisNs can be null
                    if (thisNs == null) {
                        return false;
                    }
                    final ArrayList<SchemaTypeInfo> infosLst = new ArrayList<SchemaTypeInfo>(infos);
                    Collections.sort(infosLst);
                    final Map<String, Set<XmlFile>> nsMap = new HashMap<String, Set<XmlFile>>();
                    for (final SchemaTypeInfo info : infosLst) {
                        Set<XmlFile> targetFiles = nsMap.get(info.getNamespaceUri());
                        if (targetFiles == null) {
                            targetFiles = new HashSet<>();
                            if (Comparing.equal(info.getNamespaceUri(), thisNs)) {
                                targetFiles.add(file);
                            }
                            final Collection<XmlFile> files =
                                ApplicationManager.getApplication().runReadAction(new Computable<Collection<XmlFile>>() {
                                    @Override
                                    public Collection<XmlFile> compute() {
                                        return XmlUtil.findNSFilesByURI(info.getNamespaceUri(), project, module);
                                    }
                                });
                            if (files != null) {
                                targetFiles.addAll(files);
                            }
                            nsMap.put(info.getNamespaceUri(), targetFiles);
                        }
                        if (!targetFiles.isEmpty()) {
                            for (final XmlFile targetFile : targetFiles) {
                                ApplicationManager.getApplication().runReadAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        final String prefixByURI = XmlUtil.findNamespacePrefixByURI(targetFile, info.getNamespaceUri());
                                        if (prefixByURI == null) {
                                            return;
                                        }
                                        final PsiElementProcessor processor = new PsiElementProcessor() {
                                            @Override
                                            public boolean execute(@Nonnull PsiElement element) {
                                                if (element instanceof XmlTagImpl) {
                                                    if (isCertainTypeElement(
                                                        (XmlTagImpl)element,
                                                        info.getTagName(),
                                                        prefixByURI
                                                    ) || isElementWithEmbeddedType((XmlTagImpl)element,
                                                        info.getTagName(), prefixByURI
                                                    )) {
                                                        consumer.process(element);
                                                        return false;
                                                    }
                                                }
                                                return true;
                                            }
                                        };
                                        XmlUtil.processXmlElements(targetFile, processor, true);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean isElementWithSomeEmbeddedType(XmlTagImpl xml) {
        final String localName = xml.getLocalName();
        if (!(XmlUtil.XML_SCHEMA_URI.equals(xml.getNamespace()) && "element".equals(localName))) {
            return false;
        }
        final XmlTag[] tags = xml.getSubTags();
        for (XmlTag tag : tags) {
            if (isTypeElement((XmlTagImpl)tag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isElementWithEmbeddedType(XmlTagImpl xml, final String typeName, final String typeNsPrefix) {
        final String localName = xml.getLocalName();
        if (!(XmlUtil.XML_SCHEMA_URI.equals(xml.getNamespace()) && "element".equals(localName))) {
            return false;
        }
        final XmlAttribute nameAttr = getNameAttr(xml);
        if (nameAttr == null || nameAttr.getValue() == null) {
            return false;
        }
        final String localTypeName = XmlUtil.findLocalNameByQualifiedName(nameAttr.getValue());
        final String prefix = XmlUtil.findPrefixByQualifiedName(nameAttr.getValue());
        if (!typeName.equals(localTypeName) || !typeNsPrefix.equals(prefix)) {
            return false;
        }
        final XmlTag[] tags = xml.getSubTags();
        for (XmlTag tag : tags) {
            if (isTypeElement((XmlTagImpl)tag)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCertainTypeElement(XmlTagImpl xml, final String typeName, final String nsPrefix) {
        if (!isTypeElement(xml)) {
            return false;
        }
        final XmlAttribute name = getNameAttr(xml);
        if (name == null) {
            return false;
        }
        final String value = name.getValue();
        if (value == null) {
            return false;
        }
        final String localName = XmlUtil.findLocalNameByQualifiedName(value);
        return typeName.equals(localName) && nsPrefix.equals(XmlUtil.findPrefixByQualifiedName(value));
    }

    public static boolean isTypeElement(XmlTagImpl xml) {
        final String localName = xml.getLocalName();
        return XmlUtil.XML_SCHEMA_URI.equals(xml.getNamespace()) && ("complexType".equals(localName) || "simpleType".equals(localName));
    }

    private Collection<SchemaTypeInfo> gatherInheritors(XmlTagImpl xml) {
        XmlAttribute name = getNameAttr(xml);
        if (name == null || StringUtil.isEmptyOrSpaces(name.getValue())) {
            return null;
        }
        String localName = name.getValue();
        final boolean hasPrefix = localName.contains(":");
        localName = hasPrefix ? localName.substring(localName.indexOf(':') + 1) : localName;
        final String nsPrefix = hasPrefix ? name.getValue().substring(0, name.getValue().indexOf(':')) : null;

        final XmlFile file = XmlUtil.getContainingFile(xml);
        if (file == null) {
            return null;
        }
        final Project project = file.getProject();
        if (project == null) {
            return null;
        }

        final Set<SchemaTypeInfo> result = new HashSet<>();
        final ArrayDeque<SchemaTypeInfo> queue = new ArrayDeque<>();

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

        final PairConvertor<String, String, List<Set<SchemaTypeInfo>>> worker =
            SchemaTypeInheritanceIndex.getWorker(project, file.getContainingFile().getVirtualFile());
        while (!queue.isEmpty()) {
            final SchemaTypeInfo info = queue.removeFirst();
            final List<Set<SchemaTypeInfo>> childrenOfType = worker.convert(info.getNamespaceUri(), info.getTagName());
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

    private String getDefaultNs(final XmlFile file) {
        return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            @Override
            public String compute() {
                String nsUri;
                final XmlTag tag = file.getDocument().getRootTag();
                XmlAttribute xmlns = tag.getAttribute("xmlns", XmlUtil.XML_SCHEMA_URI);
                xmlns = xmlns == null ? tag.getAttribute("xmlns") : xmlns;
                nsUri = xmlns == null ? null : xmlns.getValue();
                return nsUri;
            }
        });
    }
}
