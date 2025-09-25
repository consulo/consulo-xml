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
package consulo.xml.codeInsight.daemon.impl.analysis;

import com.intellij.xml.DefaultXmlExtension;
import com.intellij.xml.util.XmlRefCountHolder;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.ImplicitUsageProvider;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.URLReference;
import consulo.xml.psi.impl.source.xml.SchemaPrefix;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.Objects;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class XmlUnusedNamespaceInspection extends XmlSuppressableInspectionTool {
    @Nullable
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitXmlAttribute(XmlAttribute attribute) {
                if (!attribute.isNamespaceDeclaration()) {
                    checkUnusedLocations(attribute, holder);
                    return;
                }
                XmlRefCountHolder refCountHolder = XmlRefCountHolder.getRefCountHolder(attribute);
                if (refCountHolder == null) {
                    return;
                }

                String namespace = attribute.getValue();
                String declaredPrefix = getDeclaredPrefix(attribute);
                if (namespace != null && !refCountHolder.isInUse(declaredPrefix)) {
                    if (holder.getProject()
                        .getExtensionPoint(ImplicitUsageProvider.class)
                        .findFirstSafe(it -> it.isImplicitUsage(attribute)) != null) {
                        return;
                    }

                    XmlAttributeValue value = attribute.getValueElement();
                    assert value != null;
                    holder.newProblem(XmlLocalize.xmlInspectionsUnusedSchemaDeclaration())
                        .range(attribute)
                        .withFixes(new RemoveNamespaceDeclarationFix(declaredPrefix, false))
                        .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                        .create();

                    XmlTag parent = attribute.getParent();
                    if (declaredPrefix.length() == 0) {
                        XmlAttribute location = getDefaultLocation(parent);
                        if (location != null) {
                            holder.newProblem(XmlLocalize.xmlInspectionsUnusedSchemaLocation())
                                .range(location)
                                .withFixes(new RemoveNamespaceDeclarationFix(declaredPrefix, true))
                                .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                                .create();
                        }
                    }
                    else if (!refCountHolder.isUsedNamespace(namespace)) {
                        for (PsiReference reference : getLocationReferences(namespace, parent)) {
                            if (!XmlHighlightVisitor.hasBadResolve(reference, false)) {
                                holder.newProblem(XmlLocalize.xmlInspectionsUnusedSchemaLocation())
                                    .range(reference)
                                    .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                                    .withFixes(new RemoveNamespaceDeclarationFix(declaredPrefix, true))
                                    .create();
                            }
                        }
                    }
                }
            }
        };
    }

    @RequiredUIAccess
    private static void removeReferencesOrAttribute(PsiReference[] references) {
        if (references.length == 0) {
            return;
        }

        XmlAttributeValue element = (XmlAttributeValue)references[0].getElement();
        XmlAttribute attribute = (XmlAttribute)element.getParent();
        if (element.getReferences().length == references.length) { // all refs to be removed
            attribute.delete();
            return;
        }

        PsiFile file = element.getContainingFile();
        Project project = file.getProject();
        SmartPsiElementPointer<XmlAttribute> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(attribute);
        for (PsiReference reference : references) {
            RemoveNamespaceDeclarationFix.removeReferenceText(reference);
        }

        // trimming the result
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        Document document = documentManager.getDocument(file);
        assert document != null;
        documentManager.commitDocument(document);
        String trimmed = element.getValue().trim();
        XmlAttribute pointerElement = pointer.getElement();
        assert pointerElement != null;
        pointerElement.setValue(trimmed);
    }

    @RequiredReadAction
    private static void checkUnusedLocations(XmlAttribute attribute, ProblemsHolder holder) {
        if (XmlUtil.XML_SCHEMA_INSTANCE_URI.equals(attribute.getNamespace())) {
            XmlRefCountHolder refCountHolder = XmlRefCountHolder.getRefCountHolder(attribute);
            if (refCountHolder == null) {
                return;
            }

            if (XmlUtil.NO_NAMESPACE_SCHEMA_LOCATION_ATT.equals(attribute.getLocalName())) {
                if (refCountHolder.isInUse("")) {
                    return;
                }
                holder.newProblem(XmlLocalize.xmlInspectionsUnusedSchemaLocation())
                    .range(attribute)
                    .withFixes(new RemoveNamespaceLocationFix(""))
                    .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                    .create();
            }
            else if (XmlUtil.SCHEMA_LOCATION_ATT.equals(attribute.getLocalName())) {
                XmlAttributeValue value = attribute.getValueElement();
                if (value == null) {
                    return;
                }
                PsiReference[] references = value.getReferences();
                for (int i = 0, referencesLength = references.length; i < referencesLength; i++) {
                    PsiReference reference = references[i];
                    if (reference instanceof URLReference) {
                        String ns = getNamespaceFromReference(reference);
                        if (ArrayUtil.indexOf(attribute.getParent().knownNamespaces(), ns) == -1 && !refCountHolder.isUsedNamespace(ns)) {
                            if (!XmlHighlightVisitor.hasBadResolve(reference, false)) {
                                holder.newProblem(XmlLocalize.xmlInspectionsUnusedSchemaLocation())
                                    .range(reference)
                                    .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                                    .withFixes(new RemoveNamespaceLocationFix(ns))
                                    .create();
                            }
                            for (int j = i + 1; j < referencesLength; j++) {
                                PsiReference nextRef = references[j];
                                if (nextRef instanceof URLReference) {
                                    break;
                                }
                                if (!XmlHighlightVisitor.hasBadResolve(nextRef, false)) {
                                    holder.newProblem(XmlLocalize.xmlInspectionsUnusedSchemaLocation())
                                        .range(nextRef)
                                        .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                                        .withFixes(new RemoveNamespaceLocationFix(ns))
                                        .create();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static String getDeclaredPrefix(XmlAttribute attribute) {
        return attribute.getName().contains(":") ? attribute.getLocalName() : "";
    }

    @Nullable
    private static XmlAttribute getDefaultLocation(XmlTag parent) {
        return parent.getAttribute(XmlUtil.NO_NAMESPACE_SCHEMA_LOCATION_ATT, XmlUtil.XML_SCHEMA_INSTANCE_URI);
    }

    @RequiredReadAction
    private static PsiReference[] getLocationReferences(String namespace, XmlTag tag) {
        XmlAttribute locationAttr = tag.getAttribute(XmlUtil.SCHEMA_LOCATION_ATT, XmlUtil.XML_SCHEMA_INSTANCE_URI);
        if (locationAttr == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        XmlAttributeValue value = locationAttr.getValueElement();
        return value == null ? PsiReference.EMPTY_ARRAY : getLocationReferences(namespace, value);
    }

    @RequiredReadAction
    private static PsiReference[] getLocationReferences(String namespace, XmlAttributeValue value) {
        PsiReference[] references = value.getReferences();
        for (int i = 0, referencesLength = references.length; i < referencesLength; i += 2) {
            PsiReference reference = references[i];
            if (namespace.equals(getNamespaceFromReference(reference))) {
                if (i + 1 < referencesLength) {
                    return new PsiReference[]{
                        references[i + 1],
                        reference
                    };
                }
                else {
                    return new PsiReference[]{reference};
                }
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    @RequiredReadAction
    private static String getNamespaceFromReference(PsiReference reference) {
        return reference.getRangeInElement().substring(reference.getElement().getText());
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nls
    @Nonnull
    @Override
    public String getGroupDisplayName() {
        return XmlLocalize.xmlInspectionsGroupName().get();
    }

    @Nls
    @Nonnull
    @Override
    public String getDisplayName() {
        return XmlLocalize.xmlInspectionsUnusedSchema().get();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "XmlUnusedNamespaceDeclaration";
    }

    public static class RemoveNamespaceDeclarationFix implements LocalQuickFix {
        protected final String myPrefix;
        private final boolean myLocationFix;

        private RemoveNamespaceDeclarationFix(@Nullable String prefix, boolean locationFix) {
            myPrefix = prefix;
            myLocationFix = locationFix;
        }

        @Nonnull
        @Override
        public String getName() {
            return XmlLocalize.xmlInspectionsUnusedSchemaDeclarationRemove().get();
        }

        @Nonnull
        @Override
        public String getFamilyName() {
            return XmlLocalize.xmlInspectionsGroupName().get();
        }

        @Override
        @RequiredUIAccess
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            doFix(project, descriptor, true);
        }

        @Nullable
        @RequiredUIAccess
        public SmartPsiElementPointer<XmlTag> doFix(Project project, ProblemDescriptor descriptor, boolean reformat) {
            PsiElement element = descriptor.getPsiElement();
            if (element instanceof XmlAttributeValue attributeValue) {
                element = attributeValue.getParent();
            }
            else if (!(element instanceof XmlAttribute)) {
                return null;
            }
            XmlAttribute attribute = (XmlAttribute)element;
            XmlTag tag = attribute.getParent();

            if (!FileModificationService.getInstance().prepareFileForWrite(tag.getContainingFile())) {
                return null;
            }

            SmartPsiElementPointer<XmlTag> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(tag);

            doRemove(project, attribute, tag);

            if (reformat) {
                reformatStartTag(project, pointer);
            }
            return pointer;
        }

        @RequiredReadAction
        public static void reformatStartTag(Project project, SmartPsiElementPointer<XmlTag> pointer) {
            PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
            PsiFile file = pointer.getContainingFile();
            assert file != null;
            Document document = manager.getDocument(file);
            assert document != null;
            manager.commitDocument(document);
            XmlTag tag = pointer.getElement();
            assert tag != null;
            XmlUtil.reformatTagStart(tag);
        }

        @RequiredUIAccess
        protected void doRemove(Project project, XmlAttribute attribute, XmlTag parent) {
            if (!attribute.isNamespaceDeclaration()) {
                SchemaPrefix schemaPrefix = DefaultXmlExtension.DEFAULT_EXTENSION.getPrefixDeclaration(parent, myPrefix);
                if (schemaPrefix != null) {
                    attribute = schemaPrefix.getDeclaration();
                }
            }
            String namespace = attribute.getValue();
            String prefix = getDeclaredPrefix(attribute);

            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            Document document = documentManager.getDocument(attribute.getContainingFile());
            assert document != null;
            attribute.delete();
            if (prefix.length() == 0) {
                XmlAttribute locationAttr = getDefaultLocation(parent);
                if (locationAttr != null) {
                    locationAttr.delete();
                }
            }
            else {
                documentManager.doPostponedOperationsAndUnblockDocument(document);
                PsiReference[] references = getLocationReferences(namespace, parent);
                removeReferencesOrAttribute(references);
                documentManager.commitDocument(document);
            }
        }

        @RequiredUIAccess
        public static void removeReferenceText(PsiReference ref) {
            PsiElement element = ref.getElement();
            PsiFile file = element.getContainingFile();
            TextRange range = ref.getRangeInElement().shiftRight(element.getTextRange().getStartOffset());
            PsiDocumentManager manager = PsiDocumentManager.getInstance(file.getProject());
            Document document = manager.getDocument(file);
            assert document != null;
            manager.doPostponedOperationsAndUnblockDocument(document);
            document.deleteString(range.getStartOffset(), range.getEndOffset());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RemoveNamespaceDeclarationFix removeNamespaceDeclarationFix
                && Objects.equals(myPrefix, removeNamespaceDeclarationFix.myPrefix)
                && (myLocationFix || removeNamespaceDeclarationFix.myLocationFix);
        }

        @Override
        public int hashCode() {
            return myPrefix == null ? 0 : myPrefix.hashCode();
        }
    }

    public static class RemoveNamespaceLocationFix extends RemoveNamespaceDeclarationFix {
        private RemoveNamespaceLocationFix(String namespace) {
            super(namespace, true);
        }

        @Nonnull
        @Override
        public String getName() {
            return XmlLocalize.xmlInspectionsUnusedSchemaLocationRemove().get();
        }

        @Override
        @RequiredUIAccess
        protected void doRemove(Project project, XmlAttribute attribute, XmlTag parent) {
            if (myPrefix.isEmpty()) {
                attribute.delete();
            }
            else {
                XmlAttributeValue value = attribute.getValueElement();
                if (value == null) {
                    return;
                }
                PsiReference[] references = getLocationReferences(myPrefix, value);
                removeReferencesOrAttribute(references);
            }
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }
}
