// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.xml.codeInsight.editorActions;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementDescriptorWithCDataContent;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterIterator;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.action.BraceMatchingUtil;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.*;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.application.options.editor.XmlEditorOptions;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.impl.source.xml.XmlTokenImpl;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

@ExtensionImpl(id = "xmlGt", order = "after xmlEq")
public class XmlGtTypedHandler extends TypedHandlerDelegate {
    private static final Logger LOG = Logger.getInstance(XmlGtTypedHandler.class);

    @Nonnull
    @Override
    @RequiredReadAction
    public Result beforeCharTyped(
        char c,
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull PsiFile editedFile,
        @Nonnull FileType fileType
    ) {
        XmlEditorOptions xmlEditorOptions = XmlEditorOptions.getInstance();
        if (c == '>' && xmlEditorOptions.isAutomaticallyInsertClosingTag() && fileContainsXmlLanguage(editedFile)) {
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            FileViewProvider provider = editedFile.getViewProvider();
            int offset = editor.getCaretModel().getOffset();

            PsiElement element, elementAtCaret = null;

            if (offset < editor.getDocument().getTextLength()) {
                elementAtCaret = element = provider.findElementAt(offset, XMLLanguage.class);

                if (element == null && offset > 0) {
                    // seems like a template language
                    // <xml_code><caret><outer_element>
                    elementAtCaret = element = provider.findElementAt(offset - 1, XMLLanguage.class);
                }
                if (!(element instanceof PsiWhiteSpace)) {
                    boolean nonAcceptableDelimiter = true;

                    if (element instanceof XmlToken token) {
                        IElementType tokenType = token.getTokenType();

                        if (tokenType == XmlTokenType.XML_START_TAG_START || tokenType == XmlTokenType.XML_END_TAG_START) {
                            if (offset > 0) {
                                PsiElement previousElement = provider.findElementAt(offset - 1, XMLLanguage.class);

                                if (previousElement instanceof XmlToken prevToken) {
                                    tokenType = prevToken.getTokenType();
                                    element = previousElement;
                                    nonAcceptableDelimiter = false;
                                }
                            }
                        }
                        else if (tokenType == XmlTokenType.XML_NAME || tokenType == XmlTokenType.XML_TAG_NAME) {
                            if (element.getNextSibling() instanceof PsiErrorElement) {
                                nonAcceptableDelimiter = false;
                            }
                        }

                        if (tokenType == XmlTokenType.XML_TAG_END || tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END
                            && element.getTextOffset() == offset - 1) {
                            EditorModificationUtil.moveCaretRelatively(editor, 1);
                            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                            return Result.STOP;
                        }
                    }
                    if (nonAcceptableDelimiter) {
                        return Result.CONTINUE;
                    }
                }
                else {
                    // check if right after empty end
                    PsiElement previousElement = provider.findElementAt(offset - 1, XMLLanguage.class);
                    if (previousElement instanceof XmlToken prevToken) {
                        IElementType tokenType = prevToken.getTokenType();

                        if (tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END) {
                            return Result.STOP;
                        }
                    }
                }

                PsiElement parent = element.getParent();
                if (parent instanceof XmlText xmlText) {
                    String text = xmlText.getText();
                    // check /
                    int index = offset - xmlText.getTextOffset() - 1;

                    if (index >= 0 && text.charAt(index) == '/') {
                        return Result.CONTINUE; // already seen /
                    }
                    element = xmlText.getPrevSibling();
                }
                else if (parent instanceof XmlTag tag && !(element.getPrevSibling() instanceof XmlTag)
                    && !(element.getPrevSibling() instanceof OuterLanguageElement)) {
                    element = tag;
                }
                else if (parent instanceof XmlAttributeValue attributeValue) {
                    element = attributeValue;
                }
            }
            else {
                element = provider.findElementAt(editor.getDocument().getTextLength() - 1, XMLLanguage.class);
                if (element == null) {
                    return Result.CONTINUE;
                }
                element = element.getParent();
            }

            if (offset > 0 && offset <= editor.getDocument().getTextLength()) {
                if (editor.getDocument().getCharsSequence().charAt(offset - 1) == '/') {
                    // Some languages (e.g. GSP) allow character '/' in tag name.
                    return Result.CONTINUE;
                }
            }

            if (element instanceof XmlAttributeValue attributeValue) {
                element = attributeValue.getParent().getParent();
            }

            while (element instanceof PsiWhiteSpace || element instanceof OuterLanguageElement) {
                element = element.getPrevSibling();
            }
            if (element instanceof XmlDocument document) {   // hack for closing tags in RHTML
                element = document.getLastChild();
            }
            if (element == null) {
                return Result.CONTINUE;
            }
            if (!(element instanceof XmlTag)) {
                if (element instanceof XmlTokenImpl
                    && element.getPrevSibling() != null
                    && element.getPrevSibling().getText().equals("<")) {
                    // tag is started and there is another text in the end
                    EditorModificationUtil.insertStringAtCaret(editor, "</" + element.getText() + ">", false, 0);
                }
                return Result.CONTINUE;
            }

            XmlTag tag = (XmlTag)element;
            if (XmlUtil.getTokenOfType(tag, XmlTokenType.XML_TAG_END) != null) {
                return Result.CONTINUE;
            }
            if (XmlUtil.getTokenOfType(tag, XmlTokenType.XML_EMPTY_ELEMENT_END) != null) {
                return Result.CONTINUE;
            }
            XmlToken startToken = XmlUtil.getTokenOfType(tag, XmlTokenType.XML_START_TAG_START);
            if (startToken == null || !startToken.getText().equals("<")) {
                return Result.CONTINUE;
            }

            String name = tag.getName();
            if (elementAtCaret instanceof XmlToken tokenAtCaret
                && (tokenAtCaret.getTokenType() == XmlTokenType.XML_NAME
                || tokenAtCaret.getTokenType() == XmlTokenType.XML_TAG_NAME)) {
                name = name.substring(0, offset - elementAtCaret.getTextOffset());
            }
            if (tag instanceof HtmlTag && HtmlUtil.isSingleHtmlTag(tag, true)) {
                return Result.CONTINUE;
            }
            if (name.isEmpty()) {
                return Result.CONTINUE;
            }

            int tagOffset = tag.getTextRange().getStartOffset();

            XmlToken nameToken = XmlUtil.getTokenOfType(tag, XmlTokenType.XML_NAME);
            if (nameToken != null && nameToken.getTextRange().getStartOffset() > offset) {
                return Result.CONTINUE;
            }

            HighlighterIterator iterator = editor.getHighlighter().createIterator(tagOffset);
            if (BraceMatchingUtil.matchBrace(editor.getDocument().getCharsSequence(), editedFile.getFileType(), iterator, true, true)) {
                PsiElement parent = tag.getParent();
                boolean hasBalance = true;
                loop:
                while (parent instanceof XmlTag parentTag) {
                    if (name.equals(parentTag.getName())) {
                        hasBalance = false;
                        ASTNode astNode = XmlChildRole.CLOSING_TAG_NAME_FINDER.findChild(parentTag.getNode());
                        if (astNode == null) {
                            hasBalance = true;
                            break;
                        }
                        for (PsiElement el = parentTag.getNextSibling(); el != null; el = el.getNextSibling()) {
                            if (el instanceof PsiErrorElement && el.getText().startsWith("</" + name)) {
                                hasBalance = true;
                                break loop;
                            }
                        }
                    }
                    parent = parentTag.getParent();
                }
                if (hasBalance) {
                    return Result.CONTINUE;
                }
            }

            Collection<TextRange> cdataReformatRanges = null;
            XmlElementDescriptor descriptor = tag.getDescriptor();

            EditorModificationUtil.insertStringAtCaret(editor, "</" + name + ">", false, 0);

            if (descriptor instanceof XmlElementDescriptorWithCDataContent cDataContainer) {
                cdataReformatRanges = new SmartList<>();
                if (cDataContainer.requiresCdataBracesInContext(tag)) {
                    String cDataStart = "><![CDATA[";
                    String inserted = cDataStart + "\n]]>";
                    EditorModificationUtil.insertStringAtCaret(editor, inserted, false, cDataStart.length());
                    int caretOffset = editor.getCaretModel().getOffset();
                    if (caretOffset >= cDataStart.length()) {
                        cdataReformatRanges.add(TextRange.from(caretOffset - cDataStart.length(), inserted.length() + 1));
                    }
                }
            }

            if (cdataReformatRanges != null && !cdataReformatRanges.isEmpty()) {
                PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
                try {
                    CodeStyleManager.getInstance(project).reformatText(file, cdataReformatRanges);
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
            }
            return cdataReformatRanges != null && !cdataReformatRanges.isEmpty() ? Result.STOP : Result.CONTINUE;
        }
        return Result.CONTINUE;
    }

    @RequiredReadAction
    public static boolean fileContainsXmlLanguage(@Nullable PsiFile editedFile) {
        if (editedFile == null) {
            return false;
        }
        if (editedFile.getLanguage() instanceof XMLLanguage) {
            return true;
        }
        if (HtmlUtil.supportsXmlTypedHandlers(editedFile)) {
            return true;
        }
        FileViewProvider provider = editedFile.getViewProvider();
        return provider.getBaseLanguage() instanceof XMLLanguage
            || provider instanceof TemplateLanguageFileViewProvider templateLanguageFileViewProvider
            && templateLanguageFileViewProvider.getTemplateDataLanguage() instanceof XMLLanguage;
    }
}