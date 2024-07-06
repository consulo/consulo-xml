/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.xml.*;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.util.AnchorReference;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlTagUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.application.progress.ProgressManager;
import consulo.application.util.UserDataCache;
import consulo.colorScheme.TextAttributes;
import consulo.component.util.localize.BundleBase;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.QuickFixAction;
import consulo.language.editor.intention.QuickFixActionRegistrar;
import consulo.language.editor.intention.UnresolvedReferenceQuickFixProvider;
import consulo.language.editor.rawHighlight.*;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.path.FileReferenceOwner;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import consulo.xml.IdeValidationHost;
import consulo.xml.Validator;
import consulo.xml.codeInspection.XmlQuickFixFactory;
import consulo.xml.codeInspection.htmlInspections.BaseXmlEntitiesInspectionState;
import consulo.xml.codeInspection.htmlInspections.RequiredAttributesInspectionBase;
import consulo.xml.codeInspection.htmlInspections.XmlEntitiesInspection;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.lang.dtd.DTDLanguage;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mike
 */
public class XmlHighlightVisitor extends XmlElementVisitor implements HighlightVisitor, IdeValidationHost {
  private static final Logger LOG = Logger.getInstance("XmlHighlightVisitor");
  private static final UserDataCache<Boolean, PsiElement, Object> DO_NOT_VALIDATE = new UserDataCache<>("do not validate") {
      @Override
      protected Boolean compute(PsiElement parent, Object p) {
        OuterLanguageElement element = PsiTreeUtil.getChildOfType(parent, OuterLanguageElement.class);

        if (element == null) {
          // JspOuterLanguageElement is located under XmlText
          for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof XmlText) {
              element = PsiTreeUtil.getChildOfType(child, OuterLanguageElement.class);
              if (element != null) {
                break;
              }
            }
          }
        }
        if (element == null) {
          return false;
        }
        PsiFile containingFile = parent.getContainingFile();
        return containingFile.getViewProvider().getBaseLanguage() != containingFile.getLanguage();
      }
    };
  private static boolean ourDoJaxpTesting;

  private static final TextAttributes NONEMPTY_TEXT_ATTRIBUTES = new TextAttributes() {
    @Override
    public boolean isEmpty() {
      return false;
    }
  };
  private HighlightInfoHolder myHolder;

  public XmlHighlightVisitor() {
  }

  private void addElementsForTag(XmlTag tag, @Nonnull String localizedMessage, HighlightInfoType type, IntentionAction quickFixAction) {
    addElementsForTagWithManyQuickFixes(tag, localizedMessage, type, quickFixAction);
  }

  private void addElementsForTagWithManyQuickFixes(XmlTag tag,
                                                   @Nonnull String localizedMessage,
                                                   HighlightInfoType type,
                                                   IntentionAction... quickFixActions) {
    bindMessageToTag(tag, type, -1, localizedMessage, quickFixActions);
  }

  @Override
  public void visitXmlToken(XmlToken token) {
    IElementType tokenType = token.getTokenType();
    if (tokenType == XmlTokenType.XML_NAME || tokenType == XmlTokenType.XML_TAG_NAME) {
      PsiElement element = token.getPrevSibling();
      while (element instanceof PsiWhiteSpace) {
        element = element.getPrevSibling();
      }

      if (element instanceof XmlToken) {
        if (((XmlToken)element).getTokenType() == XmlTokenType.XML_START_TAG_START) {
          PsiElement parent = element.getParent();

          if (parent instanceof XmlTag && !(token.getNextSibling() instanceof OuterLanguageElement)) {
            checkTag((XmlTag)parent);
          }
        }
      }
      else {
        PsiElement parent = token.getParent();

        if (parent instanceof XmlAttribute && !(token.getNextSibling() instanceof OuterLanguageElement)) {
          checkAttribute((XmlAttribute)parent);
        }
      }
    }
    else if (tokenType == XmlTokenType.XML_DATA_CHARACTERS && token.getParent() instanceof XmlText) {
      if (token.textContains(']') && token.textContains('>')) {

        String s = token.getText();
        String marker = "]]>";
        int i = s.indexOf(marker);

        if (i != -1) {                              // TODO: fix
          XmlTag tag = PsiTreeUtil.getParentOfType(token, XmlTag.class);
          if (tag != null && XmlExtension.getExtensionByElement(tag).shouldBeHighlightedAsTag(tag) && !skipValidation(tag)) {
            TextRange textRange = token.getTextRange();
            int start = textRange.getStartOffset() + i;
            HighlightInfoType type = tag instanceof HtmlTag ? HighlightInfoType.WARNING : HighlightInfoType.ERROR;
            LocalizeValue description = XmlErrorLocalize.cdataEndShouldNotAppearInContentUnlessToMarkEndOfCdataSection();
            HighlightInfo info =
              HighlightInfo.newHighlightInfo(type).range(start, start + marker.length()).descriptionAndTooltip(description).create();
            addToResults(info);
          }
        }
      }
    }
  }

  private void checkTag(XmlTag tag) {
    if (ourDoJaxpTesting) {
      return;
    }

    if (!myHolder.hasErrorResults()) {
      checkTagByDescriptor(tag);
    }

    if (!myHolder.hasErrorResults()) {
      if (!skipValidation(tag)) {
        final XmlElementDescriptor descriptor = tag.getDescriptor();

        if (tag instanceof HtmlTag && (descriptor instanceof AnyXmlElementDescriptor || descriptor == null)) {
          return;
        }

        checkReferences(tag);
      }
    }
  }

  private void bindMessageToTag(final XmlTag tag,
                                final HighlightInfoType warning,
                                final int messageLength,
                                @Nonnull String localizedMessage,
                                IntentionAction... quickFixActions) {
    XmlToken childByRole = XmlTagUtil.getStartTagNameElement(tag);

    bindMessageToAstNode(childByRole, warning, 0, messageLength, localizedMessage, quickFixActions);
    childByRole = XmlTagUtil.getEndTagNameElement(tag);
    bindMessageToAstNode(childByRole, warning, 0, messageLength, localizedMessage, quickFixActions);
  }


  @Override
  public void visitXmlProcessingInstruction(XmlProcessingInstruction processingInstruction) {
    super.visitXmlProcessingInstruction(processingInstruction);
    PsiElement parent = processingInstruction.getParent();

    if (parent instanceof XmlProlog && processingInstruction.getText().startsWith("<?xml")) {
      for (PsiElement e = PsiTreeUtil.prevLeaf(processingInstruction); e != null; e = PsiTreeUtil.prevLeaf(e)) {
        if (e instanceof PsiWhiteSpace && PsiTreeUtil.prevLeaf(e) != null || e instanceof OuterLanguageElement) {
          continue;
        }
        PsiElement eParent = e.getParent();
        if (eParent instanceof PsiComment) {
          e = eParent;
        }
        if (eParent instanceof XmlProcessingInstruction) {
          break;
        }

        LocalizeValue description = XmlErrorLocalize.xmlDeclarationShouldPrecedeAllDocumentContent();
        addToResults(HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(e).descriptionAndTooltip(description).create());
      }
    }
    checkReferences(processingInstruction);
  }

  private void bindMessageToAstNode(
    final PsiElement childByRole,
    final HighlightInfoType warning,
    final int offset,
    int length,
    @Nonnull String localizedMessage,
    IntentionAction... quickFixActions
  ) {
    if (childByRole != null) {
      final TextRange textRange = childByRole.getTextRange();
      if (length == -1) {
        length = textRange.getLength();
      }
      final int startOffset = textRange.getStartOffset() + offset;

      HighlightInfo highlightInfo = HighlightInfo.newHighlightInfo(warning)
                                                 .range(childByRole, startOffset, startOffset + length)
                                                 .descriptionAndTooltip(localizedMessage)
                                                 .create();

      if (highlightInfo == null) {
        highlightInfo = HighlightInfo.newHighlightInfo(warning)
                                     .range(new TextRange(startOffset, startOffset + length))
                                     .textAttributes(NONEMPTY_TEXT_ATTRIBUTES)
                                     .descriptionAndTooltip
                                       (localizedMessage)
                                     .create();
      }

      for (final IntentionAction quickFixAction : quickFixActions) {
        if (quickFixAction == null) {
          continue;
        }
        QuickFixAction.registerQuickFixAction(highlightInfo, textRange, quickFixAction);
      }
      addToResults(highlightInfo);
    }
  }

  private void checkTagByDescriptor(final XmlTag tag) {
    String name = tag.getName();

    XmlElementDescriptor elementDescriptor;

    final PsiElement parent = tag.getParent();
    if (parent instanceof XmlTag) {
      XmlTag parentTag = (XmlTag)parent;

      elementDescriptor = XmlUtil.getDescriptorFromContext(tag);

      final XmlElementDescriptor parentDescriptor = parentTag.getDescriptor();

      if (parentDescriptor != null && elementDescriptor == null && shouldBeValidated(tag)) {
        if (tag instanceof HtmlTag) {
          //XmlEntitiesInspection inspection = getInspectionProfile(tag, HtmlStyleLocalInspection.SHORT_NAME);
          //if (inspection != null /*&& isAdditionallyDeclared(inspection.getAdditionalEntries(XmlEntitiesInspection.UNKNOWN_TAG), name)*/) {
          return;
          //}
        }

        addElementsForTag(tag, XmlErrorLocalize.elementIsNotAllowedHere(name).get(), getTagProblemInfoType(tag), null);
        return;
      }

      if (elementDescriptor instanceof AnyXmlElementDescriptor || elementDescriptor == null) {
        elementDescriptor = tag.getDescriptor();
      }

      if (elementDescriptor == null) {
        return;
      }
    }
    else {
      //root tag
      elementDescriptor = tag.getDescriptor();

      if (elementDescriptor == null) {
        addElementsForTag(tag, XmlErrorLocalize.elementMustBeDeclared(name).get(), HighlightInfoType.WRONG_REF, null);
        return;
      }
    }

    if (!(elementDescriptor instanceof XmlHighlightingAwareElementDescriptor) || ((XmlHighlightingAwareElementDescriptor)elementDescriptor).shouldCheckRequiredAttributes()) {
      checkRequiredAttributes(tag, name, elementDescriptor);
    }

    if (elementDescriptor instanceof Validator) {
      //noinspection unchecked
      ((Validator<XmlTag>)elementDescriptor).validate(tag, this);
    }
  }

  private void checkRequiredAttributes(XmlTag tag, String name, XmlElementDescriptor elementDescriptor) {
    XmlAttributeDescriptor[] attributeDescriptors = elementDescriptor.getAttributesDescriptors(tag);
    Set<String> requiredAttributes = null;

    for (XmlAttributeDescriptor attribute : attributeDescriptors) {
      if (attribute != null && attribute.isRequired()) {
        if (requiredAttributes == null) {
          requiredAttributes = new HashSet<>();
        }
        requiredAttributes.add(attribute.getName(tag));
      }
    }

    if (requiredAttributes != null) {
      for (final String attrName : requiredAttributes) {
        if (!hasAttribute(tag, attrName) && !XmlExtension.getExtension(tag.getContainingFile())
                                                         .isRequiredAttributeImplicitlyPresent(tag, attrName)) {

          IntentionAction insertRequiredAttributeIntention = XmlQuickFixFactory.getInstance().insertRequiredAttributeFix(tag, attrName);
          final LocalizeValue localizedMessage = XmlErrorLocalize.elementDoesntHaveRequiredAttribute(name, attrName);
          final InspectionProfile profile = InspectionProjectProfileManager.getInstance(tag.getProject()).getInspectionProfile();
          BaseXmlEntitiesInspectionState state = profile.getToolState(XmlEntitiesInspection.REQUIRED_ATTRIBUTES_SHORT_NAME, tag);
          if (state != null) {
            reportOneTagProblem(
              tag,
              attrName,
              localizedMessage.get(),
              insertRequiredAttributeIntention,
              HighlightDisplayKey.find(XmlEntitiesInspection.REQUIRED_ATTRIBUTES_SHORT_NAME),
              state,
              RequiredAttributesInspectionBase.getIntentionAction(attrName)
            );
          }
        }
      }
    }
  }

  private static boolean hasAttribute(XmlTag tag, String attrName) {
    final XmlAttribute attribute = tag.getAttribute(attrName);
    if (attribute == null) {
      return false;
    }
    if (attribute.getValueElement() != null) {
      return true;
    }
    if (!(tag instanceof HtmlTag)) {
      return false;
    }
    final XmlAttributeDescriptor descriptor = attribute.getDescriptor();
    return descriptor != null && HtmlUtil.isBooleanAttribute(descriptor, tag);
  }

  private void reportOneTagProblem(final XmlTag tag,
                                   final String name,
                                   @Nonnull String localizedMessage,
                                   final IntentionAction basicIntention,
                                   final HighlightDisplayKey key,
                                   final BaseXmlEntitiesInspectionState state,
                                   final IntentionAction addAttributeFix) {
    boolean htmlTag = false;

    if (tag instanceof HtmlTag) {
      htmlTag = true;
      if (isAdditionallyDeclared(state, name)) {
        return;
      }
    }

    final InspectionProfile profile = InspectionProjectProfileManager.getInstance(tag.getProject()).getInspectionProfile();
    if (htmlTag && profile.isToolEnabled(key, tag)) {
      addElementsForTagWithManyQuickFixes(tag,
                                          localizedMessage,
                                          isInjectedWithoutValidation(tag) ? HighlightInfoType.INFORMATION : SeverityRegistrar.getSeverityRegistrar(
                                            tag.getProject())
                                                                                                                              .getHighlightInfoTypeBySeverity(
                                                                                                                                profile.getErrorLevel(
                                                                                                                                  key,
                                                                                                                                  tag)
                                                                                                                                       .getSeverity()),
                                          addAttributeFix,
                                          basicIntention);
    }
    else if (!htmlTag) {
      addElementsForTag(tag, localizedMessage, HighlightInfoType.ERROR, basicIntention);
    }
  }

  private static boolean isAdditionallyDeclared(final BaseXmlEntitiesInspectionState<?> state, String name) {
    for (String entry : state.getEntities()) {
      if (name.equalsIgnoreCase(entry)) {
        return true;
      }
    }
    return false;
  }

  private static HighlightInfoType getTagProblemInfoType(XmlTag tag) {
    if (tag instanceof HtmlTag && XmlUtil.HTML_URI.equals(tag.getNamespace())) {
      if (isInjectedWithoutValidation(tag)) {
        return HighlightInfoType.INFORMATION;
      }
      return HighlightInfoType.WARNING;
    }
    return HighlightInfoType.WRONG_REF;
  }

  public static boolean isInjectedWithoutValidation(PsiElement element) {
    PsiElement context = InjectedLanguageManager.getInstance(element.getProject()).getInjectionHost(element.getContainingFile());
    return context != null && skipValidation(context);
  }

  public static boolean skipValidation(PsiElement context) {
    return DO_NOT_VALIDATE.get(context, null);
  }

  public static void setSkipValidation(@Nonnull PsiElement element) {
    DO_NOT_VALIDATE.put(element, Boolean.TRUE);
  }

  @Override
  public void visitXmlAttribute(XmlAttribute attribute) {
  }

  private void checkAttribute(XmlAttribute attribute) {
    XmlTag tag = attribute.getParent();
    if (tag == null) {
      return;
    }

    final String name = attribute.getName();
    PsiElement prevLeaf = PsiTreeUtil.prevLeaf(attribute);

    if (!(prevLeaf instanceof PsiWhiteSpace)) {
      TextRange textRange = attribute.getTextRange();
      HighlightInfoType type = tag instanceof HtmlTag ? HighlightInfoType.WARNING : HighlightInfoType.ERROR;
      LocalizeValue description = XmlErrorLocalize.attributeShouldBePrecededWithSpace();
      HighlightInfo info = HighlightInfo.newHighlightInfo(type)
        .range(textRange.getStartOffset(), textRange.getStartOffset())
        .descriptionAndTooltip(description)
        .create();
      addToResults(info);
    }

    if (attribute.isNamespaceDeclaration() || XmlUtil.XML_SCHEMA_INSTANCE_URI.equals(attribute.getNamespace())) {
      //checkReferences(attribute.getValueElement());
      return;
    }

    XmlElementDescriptor elementDescriptor = tag.getDescriptor();
    if (elementDescriptor == null || elementDescriptor instanceof AnyXmlElementDescriptor || ourDoJaxpTesting) {
      return;
    }

    XmlAttributeDescriptor attributeDescriptor = elementDescriptor.getAttributeDescriptor(attribute);

    if (attributeDescriptor == null) {
      if (!XmlUtil.attributeFromTemplateFramework(name, tag)) {
        final LocalizeValue localizedMessage = XmlErrorLocalize.attributeIsNotAllowedHere(name);
        final HighlightInfo highlightInfo = reportAttributeProblem(tag, name, attribute, localizedMessage.get());
        if (highlightInfo != null) {
          PsiFile file = tag.getContainingFile();
          if (file != null) {
            for (XmlUndefinedElementFixProvider fixProvider : XmlUndefinedElementFixProvider.EP_NAME.getExtensionList()) {
              IntentionAction[] fixes = fixProvider.createFixes(attribute);
              if (fixes != null) {
                for (IntentionAction action : fixes) {
                  QuickFixAction.registerQuickFixAction(highlightInfo, action);
                }
                break;
              }
            }
          }
        }
      }
    }
    else {
      checkDuplicateAttribute(tag, attribute);

      // we skip resolve of attribute references since there is separate check when taking attribute descriptors
      PsiReference[] attrRefs = attribute.getReferences();
      doCheckRefs(attribute, attrRefs, !attribute.getNamespacePrefix().isEmpty() ? 2 : 1);
    }
  }

  @Nullable
  private HighlightInfo reportAttributeProblem(final XmlTag tag,
                                               final String localName,
                                               final XmlAttribute attribute,
                                               @Nonnull String localizedMessage) {

    final RemoveAttributeIntentionFix removeAttributeIntention = new RemoveAttributeIntentionFix(localName, attribute);

    if (!(tag instanceof HtmlTag)) {
      final HighlightInfoType tagProblemInfoType = HighlightInfoType.WRONG_REF;

      final ASTNode node = SourceTreeToPsiMap.psiElementToTree(attribute);
      assert node != null;
      final ASTNode child = XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(node);
      assert child != null;
      final HighlightInfo highlightInfo =
        HighlightInfo.newHighlightInfo(tagProblemInfoType).range(child).descriptionAndTooltip(localizedMessage).create();
      addToResults(highlightInfo);

      QuickFixAction.registerQuickFixAction(highlightInfo, removeAttributeIntention);

      return highlightInfo;
    }

    return null;
  }

  private void checkDuplicateAttribute(XmlTag tag, final XmlAttribute attribute) {
    if (skipValidation(tag)) {
      return;
    }

    final XmlAttribute[] attributes = tag.getAttributes();
    final PsiFile containingFile = tag.getContainingFile();
    final XmlExtension extension =
      containingFile instanceof XmlFile ? XmlExtension.getExtension(containingFile) : DefaultXmlExtension.DEFAULT_EXTENSION;
    for (XmlAttribute tagAttribute : attributes) {
      ProgressManager.checkCanceled();
      if (attribute != tagAttribute && Comparing.strEqual(attribute.getName(), tagAttribute.getName())) {
        final String localName = attribute.getLocalName();

        if (extension.canBeDuplicated(tagAttribute)) {
          continue; // multiple import attributes are allowed in jsp directive
        }

        final ASTNode attributeNode = SourceTreeToPsiMap.psiElementToTree(attribute);
        assert attributeNode != null;
        final ASTNode attributeNameNode = XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(attributeNode);
        assert attributeNameNode != null;
        HighlightInfo highlightInfo = HighlightInfo.newHighlightInfo(getTagProblemInfoType(tag))
          .range(attributeNameNode)
          .descriptionAndTooltip(XmlErrorLocalize.duplicateAttribute(localName))
          .create();
        addToResults(highlightInfo);

        IntentionAction intentionAction = new RemoveAttributeIntentionFix(localName, attribute);

        QuickFixAction.registerQuickFixAction(highlightInfo, intentionAction);
      }
    }
  }

  @Override
  public void visitXmlDocument(final XmlDocument document) {
    if (document.getLanguage() == DTDLanguage.INSTANCE) {
      final PsiMetaData psiMetaData = document.getMetaData();
      if (psiMetaData instanceof Validator) {
        //noinspection unchecked
        ((Validator<XmlDocument>)psiMetaData).validate(document, this);
      }
    }
  }

  @Override
  public void visitXmlTag(XmlTag tag) {
  }

  @Override
  public void visitXmlAttributeValue(XmlAttributeValue value) {
    checkReferences(value);

    final PsiElement parent = value.getParent();
    if (!(parent instanceof XmlAttribute)) {
      return;
    }

    XmlAttribute attribute = (XmlAttribute)parent;

    XmlTag tag = attribute.getParent();

    XmlElementDescriptor elementDescriptor = tag.getDescriptor();
    XmlAttributeDescriptor attributeDescriptor = elementDescriptor != null ? elementDescriptor.getAttributeDescriptor(attribute) : null;

    if (attributeDescriptor != null && !skipValidation(value)) {
      String error = attributeDescriptor.validateValue(value, attribute.getValue());

      if (error != null) {
        HighlightInfoType type = getTagProblemInfoType(tag);
        addToResults(HighlightInfo.newHighlightInfo(type).range(value).descriptionAndTooltip(error).create());
      }
    }
  }

  private void checkReferences(PsiElement value) {
    if (value == null) {
      return;
    }

    doCheckRefs(value, value.getReferences(), 0);
  }

  private void doCheckRefs(final PsiElement value, final PsiReference[] references, int start) {
    for (int i = start; i < references.length; ++i) {
      PsiReference reference = references[i];
      ProgressManager.checkCanceled();
      if (isUrlReference(reference)) {
        continue;
      }
      if (!hasBadResolve(reference, false)) {
        continue;
      }
      String description = getErrorDescription(reference);

      final int startOffset = reference.getElement().getTextRange().getStartOffset();
      final TextRange referenceRange = reference.getRangeInElement();

      // logging for IDEADEV-29655
      if (referenceRange.getStartOffset() > referenceRange.getEndOffset()) {
        LOG.error("Reference range start offset > end offset:  " + reference + ", start offset: " + referenceRange.getStartOffset() + ", end offset: " + referenceRange
          .getEndOffset());
      }

      HighlightInfoType type = getTagProblemInfoType(PsiTreeUtil.getParentOfType(value, XmlTag.class));
      if (value instanceof XmlAttributeValue) {
        PsiElement parent = value.getParent();
        if (parent instanceof XmlAttribute) {
          String name = ((XmlAttribute)parent).getName().toLowerCase();
          if (type.getSeverity(null).compareTo(HighlightInfoType.WARNING.getSeverity(null)) > 0 && name.endsWith("stylename")) {
            type = HighlightInfoType.WARNING;
          }
        }
      }
      HighlightInfo info = HighlightInfo.newHighlightInfo(type)
                                        .range(startOffset + referenceRange.getStartOffset(), startOffset + referenceRange.getEndOffset())
                                        .descriptionAndTooltip
                                          (description)
                                        .create();
      addToResults(info);
      if (reference instanceof LocalQuickFixProvider) {
        LocalQuickFix[] fixes = ((LocalQuickFixProvider)reference).getQuickFixes();
        if (fixes != null) {
          InspectionManager manager = InspectionManager.getInstance(reference.getElement().getProject());
          for (LocalQuickFix fix : fixes) {
            ProblemDescriptor descriptor =
              manager.createProblemDescriptor(value, description, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, true);
            QuickFixAction.registerQuickFixAction(info, new LocalQuickFixAsIntentionAdapter(fix, descriptor));
          }
        }
      }
      UnresolvedReferenceQuickFixProvider.registerReferenceFixes(reference, QuickFixActionRegistrar.create(info));
    }
  }

  public static boolean isUrlReference(PsiReference reference) {
    return reference instanceof FileReferenceOwner || reference instanceof AnchorReference;
  }

  @Nonnull
  public static String getErrorDescription(@Nonnull PsiReference reference) {
    String message;
    if (reference instanceof EmptyResolveMessageProvider) {
      message = ((EmptyResolveMessageProvider)reference).getUnresolvedMessagePattern();
    }
    else {
      //noinspection UnresolvedPropertyKey
      message = PsiBundle.message("cannot.resolve.symbol");
    }

    String description;
    try {
      description = BundleBase.format(message, reference.getCanonicalText()); // avoid double formatting
    }
    catch (IllegalArgumentException ex) {
      // unresolvedMessage provided by third-party reference contains wrong format string (e.g. {}), tolerate it
      description = message;
    }
    return description;
  }

  public static boolean hasBadResolve(final PsiReference reference, boolean checkSoft) {
    if (!checkSoft && reference.isSoft()) {
      return false;
    }
    if (reference instanceof PsiPolyVariantReference) {
      return ((PsiPolyVariantReference)reference).multiResolve(false).length == 0;
    }
    return reference.resolve() == null;
  }

  @Override
  public void visitXmlDoctype(XmlDoctype xmlDoctype) {
    if (skipValidation(xmlDoctype)) {
      return;
    }
    checkReferences(xmlDoctype);
  }

  private void addToResults(final HighlightInfo info) {
    myHolder.add(info);
  }

  public static void setDoJaxpTesting(boolean doJaxpTesting) {
    ourDoJaxpTesting = doJaxpTesting;
  }

  @Override
  public void addMessage(PsiElement context, String message, @Nonnull ErrorType type) {
    addMessageWithFixes(context, message, type);
  }

  @Override
  public void addMessageWithFixes(final PsiElement context,
                                  final String message,
                                  @Nonnull final ErrorType type,
                                  @Nonnull final IntentionAction... fixes) {
    if (message != null && !message.isEmpty()) {
      final PsiFile containingFile = context.getContainingFile();
      final HighlightInfoType defaultInfoType =
        type == ErrorType.ERROR ? HighlightInfoType.ERROR : type == ErrorType.WARNING ? HighlightInfoType.WARNING : HighlightInfoType.WEAK_WARNING;

      if (context instanceof XmlTag && XmlExtension.getExtension(containingFile).shouldBeHighlightedAsTag((XmlTag)context)) {
        addElementsForTagWithManyQuickFixes((XmlTag)context, message, defaultInfoType, fixes);
      }
      else {
        final PsiElement contextOfFile = InjectedLanguageManager.getInstance(containingFile.getProject()).getInjectionHost(containingFile);
        final HighlightInfo highlightInfo;

        if (contextOfFile != null) {
          TextRange range = InjectedLanguageManager.getInstance(context.getProject()).injectedToHost(context, context.getTextRange());
          highlightInfo = HighlightInfo.newHighlightInfo(defaultInfoType).range(range).descriptionAndTooltip(message).create();
        }
        else {
          highlightInfo =
            HighlightInfo.newHighlightInfo(HighlightInfoType.WRONG_REF).range(context).descriptionAndTooltip(message).create();
        }

        for (final IntentionAction quickFixAction : fixes) {
          if (quickFixAction == null) {
            continue;
          }
          QuickFixAction.registerQuickFixAction(highlightInfo, quickFixAction);
        }
        addToResults(highlightInfo);
      }
    }
  }

  @Override
  public void visit(@Nonnull final PsiElement element) {
    element.accept(this);
  }

  @Override
  public boolean analyze(@Nonnull final PsiFile file,
                         final boolean updateWholeFile,
                         @Nonnull HighlightInfoHolder holder,
                         @Nonnull Runnable action) {
    myHolder = holder;
    try {
      action.run();
    }
    finally {
      myHolder = null;
    }
    return true;
  }

  public static String getUnquotedValue(XmlAttributeValue value, XmlTag tag) {
    String unquotedValue = value.getValue();

    if (tag instanceof HtmlTag) {
      unquotedValue = unquotedValue.toLowerCase();
    }

    return unquotedValue;
  }

  public static boolean shouldBeValidated(@Nonnull XmlTag tag) {
    PsiElement parent = tag.getParent();
    if (parent instanceof XmlTag) {
      return !skipValidation(parent) && !XmlUtil.tagFromTemplateFramework(tag);
    }
    return true;
  }
}
