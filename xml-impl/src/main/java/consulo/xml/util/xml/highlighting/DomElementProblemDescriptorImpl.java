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
package consulo.xml.util.xml.highlighting;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;
import consulo.xml.util.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class DomElementProblemDescriptorImpl implements DomElementProblemDescriptor {
    private static final Logger LOG = Logger.getInstance("#DomElementProblemDescriptorImpl");
    private final DomElement myDomElement;
    private final HighlightSeverity mySeverity;
    @Nonnull
    private final LocalizeValue myMessage;
    private final LocalQuickFix[] myFixes;
    private List<Annotation> myAnnotations;
    private Pair<TextRange, PsiElement> myPair;
    public static final Pair<TextRange, PsiElement> NO_PROBLEM = new Pair<>(null, null);
    private final ProblemHighlightType myHighlightType;

    @RequiredReadAction
    public DomElementProblemDescriptorImpl(@Nonnull DomElement domElement, @Nonnull LocalizeValue message, HighlightSeverity type) {
        this(domElement, message, type, LocalQuickFix.EMPTY_ARRAY);
    }

    @RequiredReadAction
    public DomElementProblemDescriptorImpl(
        @Nonnull DomElement domElement,
        @Nonnull LocalizeValue message,
        HighlightSeverity type,
        @Nullable TextRange textRange
    ) {
        this(domElement, message, type, textRange, null, LocalQuickFix.EMPTY_ARRAY);
    }

    @RequiredReadAction
    public DomElementProblemDescriptorImpl(
        @Nonnull DomElement domElement,
        @Nonnull LocalizeValue message,
        HighlightSeverity type,
        LocalQuickFix... fixes
    ) {
        this(domElement, message, type, null, null, fixes);
    }

    @RequiredReadAction
    public DomElementProblemDescriptorImpl(
        @Nonnull DomElement domElement,
        @Nonnull LocalizeValue message,
        HighlightSeverity type,
        @Nullable TextRange textRange,
        ProblemHighlightType highlightType,
        LocalQuickFix... fixes
    ) {
        myDomElement = domElement;
        XmlElement element = domElement.getXmlElement();
        if (element != null && !Application.get().isUnitTestMode()) {
            //LOG.assertTrue(element.isPhysical(), "Problems may not be created for non-physical DOM elements");
        }
        mySeverity = type;
        myMessage = message;
        myFixes = fixes;

        if (textRange != null) {
            PsiElement psiElement = getPsiElement();
            LOG.assertTrue(
                psiElement != null,
                "Problems with explicit text range can't be created for DOM elements without underlying XML element"
            );
            assert psiElement.isValid();
            myPair = Pair.create(textRange, psiElement);
        }
        myHighlightType = highlightType;
    }

    @Nonnull
    @Override
    public DomElement getDomElement() {
        return myDomElement;
    }

    @Nonnull
    @Override
    public HighlightSeverity getHighlightSeverity() {
        return mySeverity;
    }

    @Nonnull
    @Override
    public LocalizeValue getDescriptionTemplate() {
        return myMessage;
    }

    @Nonnull
    @Override
    public LocalQuickFix[] getFixes() {
        return myFixes;
    }

    @Nonnull
    @Override
    public final List<Annotation> getAnnotations() {
        if (myAnnotations == null) {
            myAnnotations = ContainerUtil.createMaybeSingletonList(DomElementsHighlightingUtil.createAnnotation(this));
        }
        return myAnnotations;
    }

    @Override
    @RequiredReadAction
    public void highlightWholeElement() {
        PsiElement psiElement = getPsiElement();
        if (psiElement instanceof XmlAttributeValue attrValue) {
            assert attrValue.isValid() : attrValue;
            PsiElement attr = attrValue.getParent();
            myPair = Pair.create(new TextRange(0, attr.getTextLength()), attr);
        }
        else if (psiElement != null) {
            assert psiElement.isValid() : psiElement;
            XmlTag tag = psiElement instanceof XmlTag tag1 ? tag1 : (XmlTag) psiElement.getParent();
            myPair = new Pair<>(new TextRange(0, tag.getTextLength()), tag);
        }
    }

    @RequiredReadAction
    public Pair<TextRange, PsiElement> getProblemRange() {
        if (myPair == null) {
            myPair = computeProblemRange();
        }
        PsiElement element = myPair.second;
        if (element != null) {
            assert element.isValid();
        }
        return myPair;
    }

    @Nonnull
    @RequiredReadAction
    protected Pair<TextRange, PsiElement> computeProblemRange() {
        PsiElement element = getPsiElement();

        if (element != null) {
            assert element.isValid() : element;
            if (element instanceof XmlTag tag) {
                return DomUtil.getProblemRange(tag);
            }

            int length = element.getTextRange().getLength();
            TextRange range = TextRange.from(0, length);
            if (element instanceof XmlAttributeValue attrValue) {
                String value = attrValue.getValue();
                if (StringUtil.isNotEmpty(value)) {
                    range = TextRange.from(attrValue.getText().indexOf(value), value.length());
                }
            }
            return Pair.create(range, element);
        }

        XmlTag tag = getParentXmlTag();
        if (tag != null) {
            return DomUtil.getProblemRange(tag);
        }
        return NO_PROBLEM;
    }

    @Override
    public String toString() {
        return myDomElement + "; " + myMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DomElementProblemDescriptorImpl that = (DomElementProblemDescriptorImpl) o;

        return Objects.equals(myDomElement, that.myDomElement)
            && myMessage.equals(that.myMessage)
            && mySeverity.equals(that.mySeverity);
    }

    @Override
    public int hashCode() {
        int result = (myDomElement != null ? myDomElement.hashCode() : 0);
        result = 31 * result + mySeverity.hashCode();
        return 31 * result + myMessage.hashCode();
    }

    @Nullable
    @RequiredReadAction
    private PsiElement getPsiElement() {
        if (myDomElement instanceof DomFileElement domFileElem) {
            return domFileElem.getFile();
        }

        if (myDomElement instanceof GenericAttributeValue attributeValue) {
            XmlAttributeValue value = attributeValue.getXmlAttributeValue();
            return value != null && StringUtil.isNotEmpty(value.getText()) ? value : attributeValue.getXmlElement();
        }
        XmlTag tag = myDomElement.getXmlTag();
        if (myDomElement instanceof GenericValue && tag != null) {
            XmlText[] textElements = tag.getValue().getTextElements();
            if (textElements.length > 0) {
                return textElements[0];
            }
        }

        return tag;
    }

    @Nullable
    private XmlTag getParentXmlTag() {
        DomElement parent = myDomElement.getParent();
        while (parent != null) {
            if (parent.getXmlTag() != null) {
                return parent.getXmlTag();
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Nullable
    @Override
    public ProblemHighlightType getHighlightType() {
        return myHighlightType;
    }
}
