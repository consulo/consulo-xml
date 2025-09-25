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

import com.intellij.xml.XmlBundle;
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
import org.jetbrains.annotations.NonNls;

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
    @NonNls
    private static final String ELEMENT_DECLARATION_NAME = "ELEMENT";

    @RequiredReadAction
    public ElementReference(final XmlElement element, final XmlElement nameElement) {
      myElement = element;
      myNameElement = nameElement;

      final int textOffset = element.getTextRange().getStartOffset();
      final int nameTextOffset = nameElement.getTextOffset();

      myRange = new TextRange(nameTextOffset - textOffset, nameTextOffset + nameElement.getTextLength() - textOffset);

    }

    @RequiredReadAction
    @Override
    public PsiElement getElement() {
      return myElement;
    }

    @Nonnull
		@RequiredReadAction
    @Override
    public TextRange getRangeInElement() {
      return myRange;
    }

    @RequiredReadAction
    @Override
    @Nullable
    public PsiElement resolve() {
      XmlElementDescriptor descriptor = DtdResolveUtil.resolveElementReference(getCanonicalText(), myElement);
      return descriptor == null ? null : descriptor.getDeclaration();
    }


    @RequiredReadAction
    @Override
    @Nonnull
    public String getCanonicalText() {
      final XmlElement nameElement = myNameElement;
      return nameElement != null ? nameElement.getText() : "";
    }

    @RequiredWriteAction
    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
      myNameElement = ElementManipulators.getManipulator(myNameElement)
                                         .handleContentChange(myNameElement,
                                                              new TextRange(0, myNameElement.getTextLength()),
                                                              newElementName);

      return null;
    }

    @RequiredWriteAction
    @Override
    public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
      return null;
    }

    @RequiredReadAction
    @Override
    public boolean isReferenceTo(PsiElement element) {
      return myElement.getManager().areElementsEquivalent(element, resolve());
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public Object[] getVariants() {
      final XmlNSDescriptor rootTagNSDescriptor = DtdResolveUtil.getNsDescriptor(myElement);
      return rootTagNSDescriptor != null ? rootTagNSDescriptor.getRootElementsDescriptors(((XmlFile)getRealFile()).getDocument()) : ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    private PsiFile getRealFile() {
      PsiFile psiFile = myElement.getContainingFile();
      if (psiFile != null) {
        psiFile = psiFile.getOriginalFile();
      }
      return psiFile;
    }

    @RequiredReadAction
    @Override
    public boolean isSoft() {
      return true;
    }

    @Override
    public LocalQuickFix[] getQuickFixes() {
      if (!canHaveAdequateFix(getElement())) {
        return LocalQuickFix.EMPTY_ARRAY;
      }

      return new LocalQuickFix[]{
        new AddDtdDeclarationFix("xml.dtd.create.dtd.element.intention.name", ELEMENT_DECLARATION_NAME, this)
      };
    }

    @Nonnull
    @Override
    public LocalizeValue buildUnresolvedMessage(@Nonnull String referenceText) {
      return LocalizeValue.localizeTODO(XmlBundle.message("xml.dtd.unresolved.element.reference", referenceText));
    }
  }

  static class EntityReference implements PsiReference, LocalQuickFixProvider, EmptyResolveMessageProvider {
    private final PsiElement myElement;
    private final TextRange myRange;
    @NonNls
    private static final String ENTITY_DECLARATION_NAME = "ENTITY";

    @RequiredReadAction
    EntityReference(PsiElement element) {
      myElement = element;
      if (element instanceof XmlEntityRef) {
        final PsiElement child = element.getLastChild();
        final int startOffsetInParent = child.getStartOffsetInParent();
        myRange = new TextRange(startOffsetInParent + 1, startOffsetInParent + child.getTextLength() - 1);
      }
      else {
        myRange = new TextRange(1, myElement.getTextLength() - 1);
      }
    }

    @RequiredReadAction
    @Override
    public PsiElement getElement() {
      return myElement;
    }

    @Nonnull
		@RequiredReadAction
    @Override
    public TextRange getRangeInElement() {
      return myRange;
    }

    @RequiredReadAction
    @Override
    @Nullable
    public PsiElement resolve() {
      XmlEntityDecl xmlEntityDecl = XmlEntityRefImpl.resolveEntity((XmlElement)myElement,
                                                                   (myElement instanceof XmlEntityRef ? myElement.getLastChild() : myElement).getText(),
                                                                   myElement
                                                                     .getContainingFile());

      if (xmlEntityDecl != null && !xmlEntityDecl.isPhysical()) {
        PsiNamedElement element = XmlUtil.findRealNamedElement(xmlEntityDecl);
        if (element != null) {
          xmlEntityDecl = (XmlEntityDecl)element;
        }
      }
      return xmlEntityDecl;
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public String getCanonicalText() {
      return myRange.substring(myElement.getText());
    }

    @RequiredWriteAction
    @Override
    public PsiElement handleElementRename(String newElementName) throws consulo.language.util.IncorrectOperationException {
      final PsiElement elementAt = myElement.findElementAt(myRange.getStartOffset());
      return ElementManipulators.getManipulator(elementAt).handleContentChange(elementAt, getRangeInElement(), newElementName);
    }

    @RequiredWriteAction
    @Override
    public PsiElement bindToElement(@Nonnull PsiElement element) throws consulo.language.util.IncorrectOperationException {
      return null;
    }

    @RequiredReadAction
    @Override
    public boolean isReferenceTo(PsiElement element) {
      return myElement.getManager().areElementsEquivalent(resolve(), element);
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public Object[] getVariants() {
      return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @RequiredReadAction
    @Override
    public boolean isSoft() {
      return false;
    }

    @Override
    public LocalQuickFix[] getQuickFixes() {
      if (!canHaveAdequateFix(getElement())) {
        return LocalQuickFix.EMPTY_ARRAY;
      }

      return new LocalQuickFix[]{
        new AddDtdDeclarationFix("xml.dtd.create.entity.intention.name",
                                 myElement.getText().charAt(myRange.getStartOffset() - 1) == '%' ? ENTITY_DECLARATION_NAME + " %" :
                                   ENTITY_DECLARATION_NAME,
                                 this)
      };
    }

    @Nonnull
    @Override
    public LocalizeValue buildUnresolvedMessage(@Nonnull String referenceText) {
      return XmlLocalize.xmlDtdUnresolvedEntityReference(referenceText);
    }
  }

  private static boolean canHaveAdequateFix(PsiElement element) {
    final PsiFile containingFile = element.getContainingFile();

    if (containingFile.getLanguage() == HTMLLanguage.INSTANCE || containingFile.getLanguage() == XHTMLLanguage.INSTANCE || containingFile.getViewProvider() instanceof
      TemplateLanguageFileViewProvider) {
      return false;
    }
    return true;
  }

  @Override
  @Nonnull
  public PsiReference[] getReferencesByElement(@Nonnull final PsiElement element, @Nonnull final ProcessingContext context) {
    XmlElement nameElement = null;

    if (element instanceof XmlDoctype) {
      nameElement = ((XmlDoctype)element).getNameElement();
    }
    else if (element instanceof XmlElementDecl) {
      nameElement = ((XmlElementDecl)element).getNameElement();
    }
    else if (element instanceof XmlAttlistDecl) {
      nameElement = ((XmlAttlistDecl)element).getNameElement();
    }
    else if (element instanceof XmlElementContentSpec) {
      final List<PsiReference> psiRefs = new ArrayList<>();
      element.accept(new PsiRecursiveElementVisitor() {
        @Override
        public void visitElement(PsiElement child) {
          if (child instanceof XmlToken && ((XmlToken)child).getTokenType() == XmlTokenType.XML_NAME) {
            psiRefs.add(new ElementReference((XmlElement)element, (XmlElement)child));
          }
          super.visitElement(child);
        }
      });
      return psiRefs.toArray(new PsiReference[psiRefs.size()]);
    }

    if (nameElement != null) {
      return new PsiReference[]{new ElementReference((XmlElement)element, nameElement)};
    }

    if (element instanceof XmlEntityRef || (element instanceof XmlToken && ((XmlToken)element).getTokenType() == XmlTokenType.XML_CHAR_ENTITY_REF)) {
      return new PsiReference[]{new EntityReference(element)};
    }

    return PsiReference.EMPTY_ARRAY;
  }

  public ElementFilter getSystemReferenceFilter() {
    return new ElementFilter() {
      @Override
      public boolean isAcceptable(Object element, PsiElement context) {
        final PsiElement parent = context.getParent();

        if ((parent instanceof XmlEntityDecl && !((XmlEntityDecl)parent).isInternalReference())) {
          PsiElement prevSibling = context.getPrevSibling();
          if (prevSibling instanceof PsiWhiteSpace) {
            prevSibling = prevSibling.getPrevSibling();
          }

          if (prevSibling instanceof XmlToken && ((XmlToken)prevSibling).getTokenType() == XmlTokenType.XML_DOCTYPE_SYSTEM || prevSibling instanceof XmlAttributeValue) {
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
