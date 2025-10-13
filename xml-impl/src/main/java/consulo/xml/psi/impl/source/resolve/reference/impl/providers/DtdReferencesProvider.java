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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.util.AddDtdDeclarationFix;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixProvider;
import consulo.language.psi.*;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ProcessingContext;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.impl.source.xml.XmlEntityRefImpl;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Maxim.Mossienko
 */
public class DtdReferencesProvider extends PsiReferenceProvider {
    static class ElementReference implements PsiReference, LocalQuickFixProvider, EmptyResolveMessageProvider {
        private final XmlElement myElement;
        private XmlElement myNameElement;
        private final TextRange myRange;
        private static final String ELEMENT_DECLARATION_NAME = "ELEMENT";

        @RequiredReadAction
        public ElementReference(XmlElement element, XmlElement nameElement) {
            myElement = element;
            myNameElement = nameElement;

            int textOffset = element.getTextRange().getStartOffset();
            int nameTextOffset = nameElement.getTextOffset();

            myRange = new TextRange(nameTextOffset - textOffset, nameTextOffset + nameElement.getTextLength() - textOffset);
        }

        @Override
        @RequiredReadAction
        public PsiElement getElement() {
            return myElement;
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public TextRange getRangeInElement() {
            return myRange;
        }

        @Nullable
        @Override
        @RequiredReadAction
        public PsiElement resolve() {
            XmlElementDescriptor descriptor = DtdResolveUtil.resolveElementReference(getCanonicalText(), myElement);
            return descriptor == null ? null : descriptor.getDeclaration();
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public String getCanonicalText() {
            XmlElement nameElement = myNameElement;
            return nameElement != null ? nameElement.getText() : "";
        }

        @Override
        @RequiredWriteAction
        public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
            myNameElement = ElementManipulators.getManipulator(myNameElement)
                .handleContentChange(myNameElement, new TextRange(0, myNameElement.getTextLength()), newElementName);

            return null;
        }

        @Override
        @RequiredWriteAction
        public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
            return null;
        }

        @Override
        @RequiredReadAction
        public boolean isReferenceTo(PsiElement element) {
            return myElement.getManager().areElementsEquivalent(element, resolve());
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public Object[] getVariants() {
            XmlNSDescriptor rootTagNSDescriptor = DtdResolveUtil.getNsDescriptor(myElement);
            return rootTagNSDescriptor != null ? rootTagNSDescriptor.getRootElementsDescriptors(((XmlFile) getRealFile()).getDocument()) : ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        private PsiFile getRealFile() {
            PsiFile psiFile = myElement.getContainingFile();
            if (psiFile != null) {
                psiFile = psiFile.getOriginalFile();
            }
            return psiFile;
        }

        @Override
        @RequiredReadAction
        public boolean isSoft() {
            return true;
        }

        @Override
        @RequiredReadAction
        public LocalQuickFix[] getQuickFixes() {
            if (!canHaveAdequateFix(getElement())) {
                return LocalQuickFix.EMPTY_ARRAY;
            }

            return new LocalQuickFix[]{
                new AddDtdDeclarationFix(XmlLocalize::xmlDtdCreateDtdElementIntentionName, ELEMENT_DECLARATION_NAME, this)
            };
        }

        @Nonnull
        @Override
        public LocalizeValue buildUnresolvedMessage(@Nonnull String referenceText) {
            return XmlLocalize.xmlDtdUnresolvedElementReference(referenceText);
        }
    }

    static class EntityReference implements PsiReference, LocalQuickFixProvider, EmptyResolveMessageProvider {
        private final PsiElement myElement;
        private final TextRange myRange;
        private static final String ENTITY_DECLARATION_NAME = "ENTITY";

        @RequiredReadAction
        EntityReference(PsiElement element) {
            myElement = element;
            if (element instanceof XmlEntityRef) {
                PsiElement child = element.getLastChild();
                int startOffsetInParent = child.getStartOffsetInParent();
                myRange = new TextRange(startOffsetInParent + 1, startOffsetInParent + child.getTextLength() - 1);
            }
            else {
                myRange = new TextRange(1, myElement.getTextLength() - 1);
            }
        }

        @Override
        @RequiredReadAction
        public PsiElement getElement() {
            return myElement;
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public TextRange getRangeInElement() {
            return myRange;
        }

        @Nullable
        @Override
        @RequiredReadAction
        public PsiElement resolve() {
            XmlEntityDecl xmlEntityDecl = XmlEntityRefImpl.resolveEntity(
                (XmlElement) myElement,
                (myElement instanceof XmlEntityRef ? myElement.getLastChild() : myElement).getText(),
                myElement.getContainingFile()
            );

            if (xmlEntityDecl != null && !xmlEntityDecl.isPhysical()) {
                PsiNamedElement element = XmlUtil.findRealNamedElement(xmlEntityDecl);
                if (element != null) {
                    xmlEntityDecl = (XmlEntityDecl) element;
                }
            }
            return xmlEntityDecl;
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public String getCanonicalText() {
            return myRange.substring(myElement.getText());
        }

        @Override
        @RequiredWriteAction
        public PsiElement handleElementRename(String newElementName) throws consulo.language.util.IncorrectOperationException {
            PsiElement elementAt = myElement.findElementAt(myRange.getStartOffset());
            return ElementManipulators.getManipulator(elementAt).handleContentChange(elementAt, getRangeInElement(), newElementName);
        }

        @Override
        @RequiredWriteAction
        public PsiElement bindToElement(@Nonnull PsiElement element) throws consulo.language.util.IncorrectOperationException {
            return null;
        }

        @Override
        @RequiredReadAction
        public boolean isReferenceTo(PsiElement element) {
            return myElement.getManager().areElementsEquivalent(resolve(), element);
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public Object[] getVariants() {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        @Override
        @RequiredReadAction
        public boolean isSoft() {
            return false;
        }

        @Override
        @RequiredReadAction
        public LocalQuickFix[] getQuickFixes() {
            if (!canHaveAdequateFix(getElement())) {
                return LocalQuickFix.EMPTY_ARRAY;
            }

            return new LocalQuickFix[]{
                new AddDtdDeclarationFix(
                    XmlLocalize::xmlDtdCreateEntityIntentionName,
                    myElement.getText().charAt(myRange.getStartOffset() - 1) == '%'
                        ? ENTITY_DECLARATION_NAME + " %"
                        : ENTITY_DECLARATION_NAME,
                    this
                )
            };
        }

        @Nonnull
        @Override
        public LocalizeValue buildUnresolvedMessage(@Nonnull String referenceText) {
            return XmlLocalize.xmlDtdUnresolvedEntityReference(referenceText);
        }
    }

    @RequiredReadAction
    private static boolean canHaveAdequateFix(PsiElement element) {
        PsiFile containingFile = element.getContainingFile();

        return containingFile.getLanguage() != HTMLLanguage.INSTANCE
            && containingFile.getLanguage() != XHTMLLanguage.INSTANCE
            && !(containingFile.getViewProvider() instanceof TemplateLanguageFileViewProvider);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiReference[] getReferencesByElement(@Nonnull final PsiElement element, @Nonnull ProcessingContext context) {
        XmlElement nameElement = null;

        if (element instanceof XmlDoctype doctype) {
            nameElement = doctype.getNameElement();
        }
        else if (element instanceof XmlElementDecl elementDecl) {
            nameElement = elementDecl.getNameElement();
        }
        else if (element instanceof XmlAttlistDecl attlistDecl) {
            nameElement = attlistDecl.getNameElement();
        }
        else if (element instanceof XmlElementContentSpec elemContentSpec) {
            final List<PsiReference> psiRefs = new ArrayList<>();
            elemContentSpec.accept(new PsiRecursiveElementVisitor() {
                @Override
                @RequiredReadAction
                public void visitElement(PsiElement child) {
                    if (child instanceof XmlToken token && token.getTokenType() == XmlTokenType.XML_NAME) {
                        psiRefs.add(new ElementReference(elemContentSpec, elemContentSpec));
                    }
                    super.visitElement(child);
                }
            });
            return psiRefs.toArray(new PsiReference[psiRefs.size()]);
        }

        if (nameElement != null) {
            return new PsiReference[]{new ElementReference((XmlElement) element, nameElement)};
        }

        if (element instanceof XmlEntityRef
            || element instanceof XmlToken token && token.getTokenType() == XmlTokenType.XML_CHAR_ENTITY_REF) {
            return new PsiReference[]{new EntityReference(element)};
        }

        return PsiReference.EMPTY_ARRAY;
    }

    public ElementFilter getSystemReferenceFilter() {
        return new ElementFilter() {
            @Override
            @RequiredReadAction
            public boolean isAcceptable(Object element, PsiElement context) {
                PsiElement parent = context.getParent();

                if (parent instanceof XmlEntityDecl entityDecl && !entityDecl.isInternalReference()) {
                    PsiElement prevSibling = context.getPrevSibling();
                    if (prevSibling instanceof PsiWhiteSpace) {
                        prevSibling = prevSibling.getPrevSibling();
                    }

                    if (prevSibling instanceof XmlToken prevToken && prevToken.getTokenType() == XmlTokenType.XML_DOCTYPE_SYSTEM
                        || prevSibling instanceof XmlAttributeValue) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean isClassAcceptable(Class hintClass) {
                return true;
            }
        };
    }
}
