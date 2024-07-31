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
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.UserDataCache;
import consulo.language.Language;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.psi.XmlRecursiveElementVisitor;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.IdReferenceProvider;
import consulo.xml.psi.impl.source.xml.PossiblePrefixReference;
import consulo.xml.psi.impl.source.xml.SchemaPrefix;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author spleaner
 */
public class XmlRefCountHolder {
    private static final Key<CachedValue<XmlRefCountHolder>> xmlRefCountHolderKey = Key.create("xml ref count holder");

    private final static UserDataCache<CachedValue<XmlRefCountHolder>, XmlFile, Object> CACHE = new UserDataCache<>() {
        protected CachedValue<XmlRefCountHolder> compute(final XmlFile file, final Object p) {
            return CachedValuesManager.getManager(file.getProject()).createCachedValue(
                () -> {
                    final XmlRefCountHolder holder = new XmlRefCountHolder();
                    final Language language = file.getViewProvider().getBaseLanguage();
                    final PsiFile psiFile = file.getViewProvider().getPsi(language);
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
    public static XmlRefCountHolder getRefCountHolder(final XmlElement element) {
        PsiFile file = element.getContainingFile();
        return file instanceof XmlFile ? CACHE.get(xmlRefCountHolderKey, (XmlFile)file, null).getValue() : null;
    }

    private XmlRefCountHolder() {
    }


    public boolean isDuplicateIdAttributeValue(@Nonnull final XmlAttributeValue value) {
        return myPossiblyDuplicateIds.contains(value);
    }

    public boolean isValidatable(@Nullable final PsiElement element) {
        return !myDoNotValidateParentsList.contains(element);
    }

    public boolean hasIdDeclaration(@Nonnull final String idRef) {
        return myId2AttributeListMap.get(idRef) != null || myAdditionallyDeclaredIds.contains(idRef);
    }

    public boolean isIdReferenceValue(@Nonnull final XmlAttributeValue value) {
        return myIdReferences.contains(value);
    }

    private void registerId(@Nonnull final String id, @Nonnull final XmlAttributeValue attributeValue, final boolean soft) {
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

    private void registerAdditionalId(@Nonnull final String id) {
        myAdditionallyDeclaredIds.add(id);
    }

    private void registerIdReference(@Nonnull final XmlAttributeValue value) {
        myIdReferences.add(value);
    }

    private void registerOuterLanguageElement(@Nonnull final PsiElement element) {
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

        private IdGatheringRecursiveVisitor(@Nonnull XmlRefCountHolder holder) {
            super(true);
            myHolder = holder;
        }

        @Override
        public void visitElement(final PsiElement element) {
            if (element instanceof OuterLanguageElement) {
                visitOuterLanguageElement(element);
            }

            super.visitElement(element);
        }

        private void visitOuterLanguageElement(@Nonnull final PsiElement element) {
            myHolder.registerOuterLanguageElement(element);
            PsiReference[] references = element.getReferences();
            for (PsiReference reference : references) {
                if (reference instanceof PossiblePrefixReference possiblePrefixReference && possiblePrefixReference.isPrefixReference()
                    && reference.resolve() instanceof SchemaPrefix schemaPrefix) {
                    myHolder.addUsedPrefix(schemaPrefix.getName());
                }
            }
        }

        @Override
        public void visitComment(final PsiComment comment) {
            doVisitAnyComment(comment);
            super.visitComment(comment);
        }

        @Override
        public void visitXmlComment(final XmlComment comment) {
            doVisitAnyComment(comment);
            super.visitXmlComment(comment);
        }

        private void doVisitAnyComment(final PsiComment comment) {
            final String id = XmlDeclareIdInCommentAction.getImplicitlyDeclaredId(comment);
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
        public void visitXmlAttributeValue(final XmlAttributeValue value) {
            final PsiElement element = value.getParent();
            if (!(element instanceof XmlAttribute)) {
                return;
            }

            final XmlAttribute attribute = (XmlAttribute)element;

            final XmlTag tag = attribute.getParent();
            if (tag == null) {
                return;
            }

            final XmlElementDescriptor descriptor = tag.getDescriptor();
            if (descriptor == null) {
                return;
            }

            final XmlAttributeDescriptor attributeDescriptor = descriptor.getAttributeDescriptor(attribute);
            if (attributeDescriptor != null) {
                if (attributeDescriptor.hasIdType()) {
                    updateMap(attribute, value, false);
                }
                else {
                    final PsiReference[] references = value.getReferences();
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

        private void updateMap(@Nonnull final XmlAttribute attribute, @Nonnull final XmlAttributeValue value, final boolean soft) {
            final String id = XmlHighlightVisitor.getUnquotedValue(value, attribute.getParent());
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
