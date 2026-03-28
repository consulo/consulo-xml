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
package com.intellij.xml.util;

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.UserDataCache;
import consulo.language.Language;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.psi.XmlRecursiveElementVisitor;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.IdReferenceProvider;
import consulo.xml.psi.impl.source.xml.PossiblePrefixReference;
import consulo.xml.psi.impl.source.xml.SchemaPrefix;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.*;

import org.jspecify.annotations.Nullable;
import java.util.*;

/**
 * @author spleaner
 */
public class XmlRefCountHolder {
    private final static UserDataCache<CachedValue<XmlRefCountHolder>, XmlFile, @Nullable Void> CACHE =
        new UserDataCache<CachedValue<XmlRefCountHolder>, XmlFile, @Nullable Void>("xml ref count holder") {
            @Override
            protected CachedValue<XmlRefCountHolder> compute(XmlFile file, @Nullable Void p) {
                return CachedValuesManager.getManager(file.getProject()).createCachedValue(
                    () -> {
                        XmlRefCountHolder holder = new XmlRefCountHolder();
                        Language language = file.getViewProvider().getBaseLanguage();
                        PsiFile psiFile = file.getViewProvider().getPsi(language);
                        assert psiFile != null;
                        psiFile.accept(new IdGatheringRecursiveVisitor(holder));
                        return new CachedValueProvider.Result<>(holder, file);
                    },
                    false
                );
            }
        };

    private final Map<String, List<Pair<XmlAttributeValue, Boolean>>> myId2AttributeListMap = new HashMap<>();
    private final Set<XmlAttributeValue> myPossiblyDuplicateIds = new HashSet<>();
    private final List<XmlAttributeValue> myIdReferences = new ArrayList<>();
    private final Set<String> myAdditionallyDeclaredIds = new HashSet<>();
    private final Set<PsiElement> myDoNotValidateParentsList = new HashSet<>();
    private final Set<String> myUsedPrefixes = new HashSet<>();
    private final Set<String> myUsedNamespaces = new HashSet<>();

    @Nullable
    public static XmlRefCountHolder getRefCountHolder(XmlElement element) {
        return element.getContainingFile() instanceof XmlFile xmlFile ? CACHE.get(xmlFile, null).getValue() : null;
    }

    private XmlRefCountHolder() {
    }


    public boolean isDuplicateIdAttributeValue(XmlAttributeValue value) {
        return myPossiblyDuplicateIds.contains(value);
    }

    public boolean isValidatable(@Nullable PsiElement element) {
        return !myDoNotValidateParentsList.contains(element);
    }

    public boolean hasIdDeclaration(String idRef) {
        return myId2AttributeListMap.get(idRef) != null || myAdditionallyDeclaredIds.contains(idRef);
    }

    public boolean isIdReferenceValue(XmlAttributeValue value) {
        return myIdReferences.contains(value);
    }

    private void registerId(String id, XmlAttributeValue attributeValue, boolean soft) {
        List<Pair<XmlAttributeValue, Boolean>> list = myId2AttributeListMap.get(id);
        if (list == null) {
            list = new ArrayList<>();
            myId2AttributeListMap.put(id, list);
        }
        else if (!soft) {
            // mark as duplicate
            List<XmlAttributeValue> notSoft = ContainerUtil.mapNotNull(list, pair -> pair.second ? null : pair.first);
            if (!notSoft.isEmpty()) {
                myPossiblyDuplicateIds.addAll(notSoft);
                myPossiblyDuplicateIds.add(attributeValue);
            }
        }

        list.add(new Pair<>(attributeValue, soft));
    }

    private void registerAdditionalId(String id) {
        myAdditionallyDeclaredIds.add(id);
    }

    private void registerIdReference(XmlAttributeValue value) {
        myIdReferences.add(value);
    }

    private void registerOuterLanguageElement(PsiElement element) {
        PsiElement parent = element.getParent();

        if (parent instanceof XmlText) {
            parent = parent.getParent();
        }

        myDoNotValidateParentsList.add(parent);
    }

    public boolean isInUse(String prefix) {
        return myUsedPrefixes.contains(prefix);
    }

    public boolean isUsedNamespace(String ns) {
        return myUsedNamespaces.contains(ns);
    }

    private static class IdGatheringRecursiveVisitor extends XmlRecursiveElementVisitor {
        private final XmlRefCountHolder myHolder;

        private IdGatheringRecursiveVisitor(XmlRefCountHolder holder) {
            super(true);
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element instanceof OuterLanguageElement) {
                visitOuterLanguageElement(element);
            }

            super.visitElement(element);
        }

        @RequiredReadAction
        private void visitOuterLanguageElement(PsiElement element) {
            myHolder.registerOuterLanguageElement(element);
            for (PsiReference reference : element.getReferences()) {
                if (reference instanceof PossiblePrefixReference possiblePrefixReference && possiblePrefixReference.isPrefixReference()
                    && reference.resolve() instanceof SchemaPrefix schemaPrefix) {
                    myHolder.addUsedPrefix(schemaPrefix.getName());
                }
            }
        }

        @Override
        @RequiredReadAction
        public void visitComment(PsiComment comment) {
            doVisitAnyComment(comment);
            super.visitComment(comment);
        }

        @Override
        @RequiredReadAction
        public void visitXmlComment(XmlComment comment) {
            doVisitAnyComment(comment);
            super.visitXmlComment(comment);
        }

        @RequiredReadAction
        private void doVisitAnyComment(PsiComment comment) {
            String id = XmlDeclareIdInCommentAction.getImplicitlyDeclaredId(comment);
            if (id != null) {
                myHolder.registerAdditionalId(id);
            }
        }

        @Override
        public void visitXmlTag(XmlTag tag) {
            myHolder.addUsedPrefix(tag.getNamespacePrefix());
            myHolder.addUsedNamespace(tag.getNamespace());
            String text = tag.getValue().getTrimmedText();
            detectPrefix(text);
            super.visitXmlTag(tag);
        }

        @Override
        public void visitXmlAttribute(XmlAttribute attribute) {
            if (!attribute.isNamespaceDeclaration()) {
                myHolder.addUsedPrefix(attribute.getNamespacePrefix());
            }
            myHolder.addUsedNamespace(attribute.getNamespace());
            super.visitXmlAttribute(attribute);
        }

        @Override
        @RequiredReadAction
        public void visitXmlAttributeValue(XmlAttributeValue value) {
            if (!(value.getParent() instanceof XmlAttribute attribute)) {
                return;
            }

            XmlTag tag = attribute.getParent();
            if (tag == null) {
                return;
            }

            XmlElementDescriptor descriptor = tag.getDescriptor();
            if (descriptor == null) {
                return;
            }

            XmlAttributeDescriptor attributeDescriptor = descriptor.getAttributeDescriptor(attribute);
            if (attributeDescriptor != null) {
                if (attributeDescriptor.hasIdType()) {
                    updateMap(attribute, value, false);
                }
                else {
                    PsiReference[] references = value.getReferences();
                    for (PsiReference r : references) {
                        if (r instanceof IdReferenceProvider.GlobalAttributeValueSelfReference /*&& !r.isSoft()*/) {
                            updateMap(attribute, value, r.isSoft());
                        }
                        else if (r instanceof SchemaPrefixReference schemaPrefixReference) {
                            SchemaPrefix prefix = schemaPrefixReference.resolve();
                            if (prefix != null) {
                                myHolder.addUsedPrefix(prefix.getName());
                            }
                        }
                    }
                }

                if (attributeDescriptor.hasIdRefType() && PsiTreeUtil.getChildOfType(value, OuterLanguageElement.class) == null) {
                    myHolder.registerIdReference(value);
                }
            }

            String s = value.getValue();
            detectPrefix(s);
            super.visitXmlAttributeValue(value);
        }

        private void detectPrefix(String s) {
            if (s != null) {
                int pos = s.indexOf(':');
                if (pos > 0) {
                    myHolder.addUsedPrefix(s.substring(0, pos));
                }
            }
        }

        @RequiredReadAction
        private void updateMap(XmlAttribute attribute, XmlAttributeValue value, boolean soft) {
            String id = XmlHighlightVisitor.getUnquotedValue(value, attribute.getParent());
            if (XmlUtil.isSimpleValue(id, value)
                && PsiTreeUtil.getChildOfType(value, OuterLanguageElement.class) == null) {
                myHolder.registerId(id, value, soft);
            }
        }
    }

    private void addUsedPrefix(String prefix) {
        myUsedPrefixes.add(prefix);
    }

    private void addUsedNamespace(String ns) {
        myUsedNamespaces.add(ns);
    }
}
