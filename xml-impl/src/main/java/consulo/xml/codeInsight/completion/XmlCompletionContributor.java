/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.xml.codeInsight.completion;

import com.intellij.xml.Html5SchemaProvider;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlEnumeratedValueReference;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.LegacyCompletionContributor;
import consulo.ide.impl.idea.codeInsight.completion.WordCompletionContributor;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XHtmlFileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.patterns.XmlPatterns;
import consulo.xml.psi.impl.source.html.dtd.HtmlElementDescriptorImpl;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.xml.util.XmlUtil.VALUE_ATTR_NAME;
import static com.intellij.xml.util.XmlUtil.findDescriptorFile;
import static consulo.language.pattern.PlatformPatterns.psiElement;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl(id = "xml")
public class XmlCompletionContributor extends CompletionContributor {
    public static final Key<Boolean> WORD_COMPLETION_COMPATIBLE = Key.create("WORD_COMPLETION_COMPATIBLE");
    public static final EntityRefInsertHandler ENTITY_INSERT_HANDLER = new EntityRefInsertHandler();

    public static final String TAG_NAME_COMPLETION_FEATURE = "tag.name.completion";
    private static final InsertHandler<LookupElementDecorator<LookupElement>> QUOTE_EATER = (context, item) -> {
        char completionChar = context.getCompletionChar();
        if (completionChar == '\'' || completionChar == '\"') {
            context.setAddCompletionChar(false);
            item.getDelegate().handleInsert(context);

            Editor editor = context.getEditor();
            Document document = editor.getDocument();
            int tailOffset = editor.getCaretModel().getOffset();
            if (document.getTextLength() > tailOffset) {
                char c = document.getCharsSequence().charAt(tailOffset);
                if (c == completionChar || completionChar == '\'') {
                    editor.getCaretModel().moveToOffset(tailOffset + 1);
                }
            }
        }
        else {
            item.getDelegate().handleInsert(context);
        }
    };

    public XmlCompletionContributor() {
        extend(
            CompletionType.BASIC,
            psiElement().inside(XmlPatterns.xmlFile()),
            (parameters, context, result) -> {
                PsiElement position = parameters.getPosition();
                IElementType type = position.getNode().getElementType();
                if (type != XmlTokenType.XML_DATA_CHARACTERS && type != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
                    return;
                }
                if ((position.getPrevSibling() != null && position.getPrevSibling().textMatches("&"))
                    || position.textContains('&')) {
                    PrefixMatcher matcher = result.getPrefixMatcher();
                    String prefix = matcher.getPrefix();
                    if (prefix.startsWith("&")) {
                        prefix = prefix.substring(1);
                    }
                    else if (prefix.contains("&")) {
                        prefix = prefix.substring(prefix.indexOf("&") + 1);
                    }

                    addEntityRefCompletions(position, result.withPrefixMatcher(prefix));
                }
            }
        );
        extend(
            CompletionType.BASIC,
            psiElement().inside(XmlPatterns.xmlAttributeValue()),
            (parameters, context, result) -> {
                PsiElement position = parameters.getPosition();
                if (!position.getLanguage().isKindOf(XMLLanguage.INSTANCE)) {
                    return;
                }
                XmlAttributeValue attributeValue = PsiTreeUtil.getParentOfType(position, XmlAttributeValue.class, false);
                if (attributeValue == null) {
                    // we are injected, only getContext() returns attribute value
                    return;
                }

                Set<String> usedWords = new HashSet<>();
                SimpleReference<Boolean> addWordVariants = SimpleReference.create(true);
                result.runRemainingContributors(
                    parameters,
                    r -> {
                        if (r.getLookupElement().getUserData(WORD_COMPLETION_COMPATIBLE) == null) {
                            addWordVariants.set(false);
                        }
                        usedWords.add(r.getLookupElement().getLookupString());
                        result.passResult(r.withLookupElement(
                            LookupElementDecorator.withInsertHandler(r.getLookupElement(), QUOTE_EATER)
                        ));
                    }
                );
                if (addWordVariants.get()) {
                    addWordVariants.set(attributeValue.getReferences().length == 0);
                }

                if (addWordVariants.get() && parameters.getInvocationCount() > 0) {
                    WordCompletionContributor.addWordCompletionVariants(result, parameters, usedWords);
                }
            }
        );
        extend(
            CompletionType.BASIC,
            psiElement().withElementType(XmlTokenType.XML_DATA_CHARACTERS),
            (parameters, context, result) -> {
                XmlTag tag = PsiTreeUtil.getParentOfType(parameters.getPosition(), XmlTag.class, false);
                if (tag != null && !hasEnumerationReference(parameters, result)) {
                    XmlTag simpleContent = XmlUtil.getSchemaSimpleContent(tag);
                    if (simpleContent != null) {
                        XmlUtil.processEnumerationValues(
                            simpleContent,
                            element -> {
                                String value = element.getAttributeValue(VALUE_ATTR_NAME);
                                assert value != null;
                                result.addElement(LookupElementBuilder.create(value));
                                return true;
                            }
                        );
                    }
                }
            }
        );
    }

    static boolean hasEnumerationReference(CompletionParameters parameters, CompletionResultSet result) {
        SimpleReference<Boolean> hasRef = SimpleReference.create(false);
        LegacyCompletionContributor.processReferences(parameters, result, (reference, resultSet) ->
        {
            if (reference instanceof XmlEnumeratedValueReference) {
                hasRef.set(true);
            }
        });
        return hasRef.get();
    }

    public static boolean isXmlNameCompletion(CompletionParameters parameters) {
        ASTNode node = parameters.getPosition().getNode();
        return node != null && node.getElementType() == XmlTokenType.XML_NAME;
    }

    @Override
    @RequiredReadAction
    public void fillCompletionVariants(@Nonnull CompletionParameters parameters, @Nonnull CompletionResultSet result) {
        super.fillCompletionVariants(parameters, result);
        if (result.isStopped()) {
            return;
        }

        PsiElement element = parameters.getPosition();

        if (parameters.isExtendedCompletion()) {
            completeTagName(parameters, result);
        }
        else if (parameters.getCompletionType() == CompletionType.SMART) {
            new XmlSmartCompletionProvider().complete(parameters, result, element);
        }
    }

    static void completeTagName(CompletionParameters parameters, CompletionResultSet result) {
        PsiElement element = parameters.getPosition();
        if (!isXmlNameCompletion(parameters)) {
            return;
        }
        result.stopHere();
        PsiElement parent = element.getParent();
        if (!(parent instanceof XmlTag) || !(parameters.getOriginalFile() instanceof XmlFile)) {
            return;
        }
        XmlTag tag = (XmlTag)parent;
        String namespace = tag.getNamespace();
        String prefix = result.getPrefixMatcher().getPrefix();
        int pos = prefix.indexOf(':');

        PsiReference reference = tag.getReference();
        String namespacePrefix = tag.getNamespacePrefix();

        if (reference != null && !namespace.isEmpty() && !namespacePrefix.isEmpty()) {
            // fallback to simple completion
            result.runRemainingContributors(parameters, true);
        }
        else {
            CompletionResultSet newResult = result.withPrefixMatcher(pos >= 0 ? prefix.substring(pos + 1) : prefix);

            XmlFile file = (XmlFile)parameters.getOriginalFile();
            List<XmlExtension.TagInfo> names = XmlExtension.getExtension(file).getAvailableTagNames(file, tag);
            for (XmlExtension.TagInfo info : names) {
                LookupElement item = createLookupElement(info, info.namespace, namespacePrefix.isEmpty() ? null : namespacePrefix);
                newResult.addElement(item);
            }
        }
    }

    public static LookupElement createLookupElement(XmlExtension.TagInfo tagInfo, String tailText, @Nullable String namespacePrefix) {
        LookupElementBuilder builder = LookupElementBuilder.create(tagInfo, tagInfo.name)
            .withInsertHandler(new ExtendedTagInsertHandler(tagInfo.name, tagInfo.namespace, namespacePrefix));
        if (!StringUtil.isEmpty(tailText)) {
            builder = builder.withTypeText(tailText, true);
        }
        return builder;
    }

    @Override
    public String advertise(@Nonnull CompletionParameters parameters) {
        if (isXmlNameCompletion(parameters) && parameters.getCompletionType() == CompletionType.BASIC) {
            if (FeatureUsageTracker.getInstance()
                .isToBeAdvertisedInLookup(TAG_NAME_COMPLETION_FEATURE, parameters.getPosition().getProject())) {
                String shortcut = getActionShortcut(IdeActions.ACTION_CODE_COMPLETION);
                return XmlLocalize.tagNameCompletionHint(shortcut).get();
            }
        }
        return super.advertise(parameters);
    }

    @Override
    @RequiredReadAction
    public void beforeCompletion(@Nonnull CompletionInitializationContext context) {
        int offset = context.getStartOffset();
        PsiFile file = context.getFile();
        XmlAttributeValue attributeValue = PsiTreeUtil.findElementOfClassAtOffset(file, offset, XmlAttributeValue.class, true);
        if (attributeValue != null && offset == attributeValue.getTextRange().getStartOffset()) {
            context.setDummyIdentifier("");
        }

        PsiElement at = file.findElementAt(offset);
        if (at != null && at.getNode().getElementType() == XmlTokenType.XML_NAME && at.getParent() instanceof XmlAttribute) {
            context.getOffsetMap().addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, at.getTextRange().getEndOffset());
        }
        if (at != null && at.getParent() instanceof XmlAttributeValue atAttrValue) {
            int end = atAttrValue.getTextRange().getEndOffset();
            Document document = context.getEditor().getDocument();
            int lineEnd = document.getLineEndOffset(document.getLineNumber(offset));
            if (lineEnd < end) {
                context.setReplacementOffset(lineEnd);
            }
        }
    }

    @RequiredReadAction
    private static void addEntityRefCompletions(PsiElement context, CompletionResultSet resultSet) {
        XmlFile containingFile = null;
        XmlFile descriptorFile = null;
        XmlTag tag = PsiTreeUtil.getParentOfType(context, XmlTag.class);

        if (tag != null) {
            containingFile = (XmlFile)tag.getContainingFile();
            descriptorFile = findDescriptorFile(tag, containingFile);
        }

        if (HtmlUtil.isHtml5Context(tag)) {
            descriptorFile = XmlUtil.findXmlFile(containingFile, Html5SchemaProvider.getCharsDtdLocation());
        }
        else if (tag == null) {
            XmlDocument document = PsiTreeUtil.getParentOfType(context, XmlDocument.class);

            if (document != null) {
                containingFile = (XmlFile)document.getContainingFile();

                FileType ft = containingFile.getFileType();
                if (HtmlUtil.isHtml5Document(document)) {
                    descriptorFile = XmlUtil.findXmlFile(containingFile, Html5SchemaProvider.getCharsDtdLocation());
                }
                else if (ft != XmlFileType.INSTANCE) {
                    String namespace = ft == XHtmlFileType.INSTANCE ? XmlUtil.XHTML_URI : XmlUtil.HTML_URI;
                    XmlNSDescriptor nsDescriptor = document.getDefaultNSDescriptor(namespace, true);

                    if (nsDescriptor != null) {
                        descriptorFile = nsDescriptor.getDescriptorFile();
                    }
                }
            }
        }

        if (descriptorFile != null && containingFile != null) {
            boolean acceptSystemEntities = containingFile.getFileType() == XmlFileType.INSTANCE;
            boolean caseInsensitive = (tag != null && tag.getDescriptor() instanceof HtmlElementDescriptorImpl)
                || containingFile.getLanguage().isKindOf(HTMLLanguage.INSTANCE);

            PsiElementProcessor processor = element -> {
                if (element instanceof XmlEntityDecl xmlEntityDecl) {
                    if (xmlEntityDecl.isInternalReference() || acceptSystemEntities) {
                        LookupElementBuilder _item = buildEntityLookupItem(xmlEntityDecl);
                        if (_item != null) {
                            resultSet.addElement(_item.withCaseSensitivity(!caseInsensitive));
                            resultSet.stopHere();
                        }
                    }
                }
                return true;
            };

            XmlUtil.processXmlElements(descriptorFile, processor, true);
            if (descriptorFile != containingFile && acceptSystemEntities) {
                XmlProlog element = containingFile.getDocument().getProlog();
                if (element != null) {
                    XmlUtil.processXmlElements(element, processor, true);
                }
            }
        }
    }

    @Nullable
    private static LookupElementBuilder buildEntityLookupItem(@Nonnull XmlEntityDecl decl) {
        String name = decl.getName();
        if (name == null) {
            return null;
        }

        LookupElementBuilder result = LookupElementBuilder.create(name).withInsertHandler(ENTITY_INSERT_HANDLER);
        XmlAttributeValue value = decl.getValueElement();
        ASTNode node = value.getNode();
        if (node != null) {
            ASTNode[] nodes = node.getChildren(TokenSet.create(XmlTokenType.XML_CHAR_ENTITY_REF));
            if (nodes.length == 1) {
                String valueText = nodes[0].getText();
                int i = valueText.indexOf('#');
                if (i > 0) {
                    String s = valueText.substring(i + 1);
                    s = StringUtil.trimEnd(s, ";");

                    try {
                        int unicodeChar = Integer.valueOf(s);
                        return result.withTypeText(String.valueOf((char)unicodeChar));
                    }
                    catch (NumberFormatException e) {
                        return result;
                    }
                }
            }
        }

        return result;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return Language.ANY;
    }

    private static class EntityRefInsertHandler implements InsertHandler<LookupElement> {
        @Override
        @RequiredUIAccess
        public void handleInsert(InsertionContext context, LookupElement item) {
            context.setAddCompletionChar(false);
            CaretModel caretModel = context.getEditor().getCaretModel();
            context.getEditor().getDocument().insertString(caretModel.getOffset(), ";");
            caretModel.moveToOffset(caretModel.getOffset() + 1);
        }
    }
}
