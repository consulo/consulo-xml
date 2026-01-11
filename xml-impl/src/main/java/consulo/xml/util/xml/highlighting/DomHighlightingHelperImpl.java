/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.xml.util.xml.highlighting;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ProcessingContext;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;
import consulo.xml.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.impl.*;
import consulo.xml.util.xml.reflect.AbstractDomChildrenDescription;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.reflect.DomGenericInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class DomHighlightingHelperImpl extends DomHighlightingHelper {
    public static final DomHighlightingHelperImpl INSTANCE = new DomHighlightingHelperImpl();
    private final GenericValueReferenceProvider myProvider = new GenericValueReferenceProvider();
    private final DomApplicationComponent myDomApplicationComponent = DomApplicationComponent.getInstance();

    @Override
    public void runAnnotators(DomElement element, DomElementAnnotationHolder holder, Class<? extends DomElement> rootClass) {
        DomElementsAnnotator annotator = myDomApplicationComponent.getAnnotator(rootClass);
        if (annotator != null) {
            annotator.annotate(element, holder);
        }
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public List<DomElementProblemDescriptor> checkRequired(DomElement element, DomElementAnnotationHolder holder) {
        Required required = element.getAnnotation(Required.class);
        if (required != null) {
            XmlElement xmlElement = element.getXmlElement();
            if (xmlElement == null) {
                if (required.value()) {
                    String xmlElementName = element.getXmlElementName();
                    if (element instanceof GenericAttributeValue) {
                        return Arrays.asList(holder.createProblem(element, IdeLocalize.attribute0ShouldBeDefined(xmlElementName).get()));
                    }
                    return Arrays.asList(
                        holder.createProblem(
                            element,
                            HighlightSeverity.ERROR,
                            IdeLocalize.childTag0ShouldBeDefined(xmlElementName).get(),
                            new AddRequiredSubtagFix(xmlElementName, element.getXmlElementNamespace(), element.getParent().getXmlTag())
                        )
                    );
                }
            }
            else if (element instanceof GenericDomValue) {
                return ContainerUtil.createMaybeSingletonList(checkRequiredGenericValue((GenericDomValue) element, required, holder));
            }
        }
        if (DomUtil.hasXml(element)) {
            List<DomElementProblemDescriptor> list = new SmartList<>();
            DomGenericInfo info = element.getGenericInfo();
            for (AbstractDomChildrenDescription description : info.getChildrenDescriptions()) {
                if (description instanceof DomCollectionChildDescription childDescription && description.getValues(element).isEmpty()) {
                    Required annotation = description.getAnnotation(Required.class);
                    if (annotation != null && annotation.value()) {
                        list.add(holder.createProblem(
                            element,
                            childDescription,
                            IdeLocalize.childTag0ShouldBeDefined(childDescription.getXmlElementName()).get()
                        ));
                    }
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public List<DomElementProblemDescriptor> checkResolveProblems(GenericDomValue element, DomElementAnnotationHolder holder) {
        if (StringUtil.isEmpty(element.getStringValue())) {
            Required required = element.getAnnotation(Required.class);
            if (required != null && !required.nonEmpty()) {
                return Collections.emptyList();
            }
        }

        XmlElement valueElement = DomUtil.getValueElement(element);
        if (valueElement != null && !isSoftReference(element)) {
            List<DomElementProblemDescriptor> list = new SmartList<>();
            PsiReference[] psiReferences = myProvider.getReferencesByElement(valueElement, new ProcessingContext());
            GenericDomValueReference domReference = ContainerUtil.findInstance(psiReferences, GenericDomValueReference.class);
            Converter converter = WrappingConverter.getDeepestConverter(element.getConverter(), element);
            boolean hasBadResolve = false;
            if (domReference == null || !isDomResolveOK(element, domReference, converter)) {
                for (PsiReference reference : psiReferences) {
                    if (reference != domReference && hasBadResolve(reference)) {
                        hasBadResolve = true;
                        list.add(holder.createResolveProblem(element, reference));
                    }
                }
                boolean isResolvingConverter = converter instanceof ResolvingConverter;
                if (!hasBadResolve
                    && (domReference != null
                    || isResolvingConverter && hasBadResolve(domReference = new GenericDomValueReference(element)))) {
                    hasBadResolve = true;
                    list.add(holder.createResolveProblem(element, domReference));
                }
            }
            if (!hasBadResolve && psiReferences.length == 0 && element.getValue() == null && !PsiTreeUtil.hasErrorElements(valueElement)) {
                LocalizeValue errorMessage = converter.buildUnresolvedMessage(
                    element.getStringValue(),
                    ConvertContextFactory.createConvertContext(DomManagerImpl.getDomInvocationHandler(element))
                );
                if (errorMessage.isNotEmpty()) {
                    list.add(holder.createProblem(element, errorMessage.get()));
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    @RequiredReadAction
    private static boolean isDomResolveOK(GenericDomValue element, GenericDomValueReference domReference, Converter converter) {
        return !hasBadResolve(domReference)
            || converter instanceof ResolvingConverter resolvingConverter
            && resolvingConverter.getAdditionalVariants(domReference.getConvertContext()).contains(element.getStringValue());
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public List<DomElementProblemDescriptor> checkNameIdentity(DomElement element, DomElementAnnotationHolder holder) {
        String elementName = ElementPresentationManager.getElementName(element);
        if (StringUtil.isNotEmpty(elementName)) {
            DomElement domElement = DomUtil.findDuplicateNamedValue(element, elementName);
            if (domElement != null) {
                String typeName = ElementPresentationManager.getTypeNameForObject(element);
                GenericDomValue genericDomValue = domElement.getGenericInfo().getNameDomElement(element);
                if (genericDomValue != null) {
                    return Arrays.asList(holder.createProblem(
                        genericDomValue,
                        DomUtil.getFile(domElement).equals(DomUtil.getFile(element))
                            ? IdeLocalize.modelHighlightingIdentity(typeName).get()
                            : IdeLocalize.modelHighlightingIdentityInOtherFile(
                                typeName,
                                domElement.getXmlTag().getContainingFile().getName()
                            ).get()
                    ));
                }
            }
        }
        return Collections.emptyList();
    }

    @RequiredReadAction
    private static boolean hasBadResolve(PsiReference reference) {
        return XmlHighlightVisitor.hasBadResolve(reference, true);
    }

    private static boolean isSoftReference(GenericDomValue value) {
        Resolve resolve = value.getAnnotation(Resolve.class);
        if (resolve != null && resolve.soft()) {
            return true;
        }

        Convert convert = value.getAnnotation(Convert.class);
        if (convert != null && convert.soft()) {
            return true;
        }

        Referencing referencing = value.getAnnotation(Referencing.class);
        return referencing != null && referencing.soft();
    }

    @Nullable
    @RequiredReadAction
    private static DomElementProblemDescriptor checkRequiredGenericValue(
        GenericDomValue child,
        Required required,
        DomElementAnnotationHolder annotator
    ) {
        String stringValue = child.getStringValue();
        if (stringValue == null) {
            return null;
        }

        if (required.nonEmpty() && isEmpty(child, stringValue)) {
            return annotator.createProblem(child, IdeLocalize.valueMustNotBeEmpty().get());
        }
        if (required.identifier() && !isIdentifier(stringValue)) {
            return annotator.createProblem(child, IdeLocalize.valueMustBeIdentifier().get());
        }
        return null;
    }

    private static boolean isIdentifier(String s) {
        if (StringUtil.isEmptyOrSpaces(s)) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }

        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    @RequiredReadAction
    private static boolean isEmpty(GenericDomValue child, String stringValue) {
        if (stringValue.trim().length() != 0) {
            return false;
        }
        if (child instanceof GenericAttributeValue genericAttributeValue) {
            XmlAttributeValue value = genericAttributeValue.getXmlAttributeValue();
            if (value != null && value.getTextRange().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static class AddRequiredSubtagFix implements LocalQuickFix, SyntheticIntentionAction {
        private final String myTagName;
        private final String myTagNamespace;
        private final XmlTag myParentTag;

        public AddRequiredSubtagFix(@Nonnull String tagName, @Nonnull String tagNamespace, @Nonnull XmlTag parentTag) {
            myTagName = tagName;
            myTagNamespace = tagNamespace;
            myParentTag = parentTag;
        }

        @Nonnull
        @Override
        public LocalizeValue getName() {
            return XmlLocalize.insertRequiredTagFix(myTagName);
        }

        @Nonnull
        @Override
        public LocalizeValue getText() {
            return getName();
        }

        @Override
        public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
            return true;
        }

        @Override
        public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            doFix();
        }

        @Override
        public boolean startInWriteAction() {
            return true;
        }

        @Override
        @RequiredUIAccess
        public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
            doFix();
        }

        private void doFix() {
            if (!FileModificationService.getInstance().prepareFileForWrite(myParentTag.getContainingFile())) {
                return;
            }

            try {
                myParentTag.add(myParentTag.createChildTag(myTagName, myTagNamespace, null, false));
            }
            catch (IncorrectOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
