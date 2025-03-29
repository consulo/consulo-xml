/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.xml.*;
import com.intellij.xml.actions.GenerateXmlTagAction;
import com.intellij.xml.impl.schema.XmlElementDescriptorImpl;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.language.ast.ASTNode;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.event.TemplateEditingAdapter;
import consulo.language.editor.template.macro.CompleteMacro;
import consulo.language.editor.template.macro.CompleteSmartMacro;
import consulo.language.editor.template.macro.MacroCallNode;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UndoManager;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.xml.application.options.editor.XmlEditorOptions;
import consulo.xml.codeInsight.editorActions.XmlTagNameSynchronizer;
import consulo.xml.codeInspection.htmlInspections.BaseXmlEntitiesInspectionState;
import consulo.xml.codeInspection.htmlInspections.XmlEntitiesInspection;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class XmlTagInsertHandler implements InsertHandler<LookupElement> {
    public static final XmlTagInsertHandler INSTANCE = new XmlTagInsertHandler();

    @Override
    @RequiredUIAccess
    public void handleInsert(InsertionContext context, LookupElement item) {
        Project project = context.getProject();
        Editor editor = context.getEditor();
        Document document = EditorWindow.getTopLevelEditor(editor).getDocument();
        int startOffset = context.getStartOffset();
        SimpleReference<PsiElement> currentElementRef = SimpleReference.create();

        // Need to insert " " to prevent creating tags like <tagThis is my text
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        // Need to insert " " to prevent creating tags like <tagThis is my text
        XmlTagNameSynchronizer.runWithoutCancellingSyncTagsEditing(
            document,
            () -> {
                int offset = editor.getCaretModel().getOffset();
                editor.getDocument().insertString(offset, " ");
                PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
                currentElementRef.set(context.getFile().findElementAt(startOffset));
                editor.getDocument().deleteString(offset, offset + 1);
            }
        );

        XmlTag tag = PsiTreeUtil.getContextOfType(currentElementRef.get(), XmlTag.class, true);

        if (tag == null) {
            return;
        }

        if (context.getCompletionChar() != Lookup.COMPLETE_STATEMENT_SELECT_CHAR) {
            context.setAddCompletionChar(false);
        }

        XmlElementDescriptor descriptor = tag.getDescriptor();

        if (XmlUtil.getTokenOfType(tag, XmlTokenType.XML_TAG_END) == null
            && XmlUtil.getTokenOfType(tag, XmlTokenType.XML_EMPTY_ELEMENT_END) == null) {

            if (descriptor != null) {
                insertIncompleteTag(context.getCompletionChar(), editor, tag);
            }
        }
        else if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            int caretOffset = editor.getCaretModel().getOffset();

            PsiElement otherTag = PsiTreeUtil.getParentOfType(context.getFile().findElementAt(caretOffset), XmlTag.class);

            PsiElement endTagStart = XmlUtil.getTokenOfType(otherTag, XmlTokenType.XML_END_TAG_START);

            if (endTagStart != null) {
                PsiElement sibling = endTagStart.getNextSibling();

                assert sibling != null;
                ASTNode node = sibling.getNode();
                assert node != null;
                if (node.getElementType() == XmlTokenType.XML_NAME) {
                    int sOffset = sibling.getTextRange().getStartOffset();
                    int eOffset = sibling.getTextRange().getEndOffset();

                    editor.getDocument().deleteString(sOffset, eOffset);
                    editor.getDocument().insertString(sOffset, ((XmlTag)otherTag).getName());
                }
            }

            editor.getCaretModel().moveToOffset(caretOffset + 1);
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            editor.getSelectionModel().removeSelection();
        }

        if (context.getCompletionChar() == ' ' && TemplateManager.getInstance(project).getActiveTemplate(editor) != null) {
            return;
        }

        TailType tailType = LookupItem.handleCompletionChar(editor, item, context.getCompletionChar());
        tailType.processTail(editor, editor.getCaretModel().getOffset());
    }

    public static void insertIncompleteTag(char completionChar, Editor editor, XmlTag tag) {
        XmlElementDescriptor descriptor = tag.getDescriptor();
        Project project = editor.getProject();
        TemplateManager templateManager = TemplateManager.getInstance(project);
        Template template = templateManager.createTemplate("", "");

        template.setToIndent(true);

        // temp code
        PsiFile containingFile = tag.getContainingFile();
        boolean htmlCode = HtmlUtil.hasHtml(containingFile) || HtmlUtil.supportsXmlTypedHandlers(containingFile);
        template.setToReformat(!htmlCode);

        StringBuilder indirectRequiredAttrs = addRequiredAttributes(descriptor, tag, template, containingFile);
        boolean chooseAttributeName = addTail(completionChar, descriptor, htmlCode, tag, template, indirectRequiredAttrs);

        templateManager.startTemplate(editor, template, new TemplateEditingAdapter() {
            private RangeMarker myAttrValueMarker;

            @Override
            public void waitingForInput(Template template) {
                int offset = editor.getCaretModel().getOffset();
                myAttrValueMarker = editor.getDocument().createRangeMarker(offset + 1, offset + 4);
            }

            @Override
            @RequiredUIAccess
            public void templateFinished(Template template, boolean brokenOff) {
                int offset = editor.getCaretModel().getOffset();

                if (chooseAttributeName && offset > 0) {
                    char c = editor.getDocument().getCharsSequence().charAt(offset - 1);
                    if (c == '/' || (c == ' ' && brokenOff)) {
                        new WriteCommandAction.Simple(project) {
                            @Override
                            protected void run() throws Throwable {
                                editor.getDocument().replaceString(offset, offset + 3, ">");
                            }
                        }.execute();
                    }
                }
            }

            @Override
            @RequiredUIAccess
            public void templateCancelled(Template template) {
                if (myAttrValueMarker == null) {
                    return;
                }

                UndoManager manager = ProjectUndoManager.getInstance(project);
                if (manager.isUndoInProgress() || manager.isRedoInProgress()) {
                    return;
                }

                if (chooseAttributeName && myAttrValueMarker.isValid()) {
                    int startOffset = myAttrValueMarker.getStartOffset();
                    int endOffset = myAttrValueMarker.getEndOffset();
                    new WriteCommandAction.Simple(project) {
                        @Override
                        protected void run() throws Throwable {
                            editor.getDocument().replaceString(startOffset, endOffset, ">");
                        }
                    }.execute();
                }
            }
        });
    }

    @Nullable
    private static StringBuilder addRequiredAttributes(
        XmlElementDescriptor descriptor,
        @Nullable XmlTag tag,
        Template template,
        PsiFile containingFile
    ) {

        boolean htmlCode = HtmlUtil.hasHtml(containingFile) || HtmlUtil.supportsXmlTypedHandlers(containingFile);
        Set<String> notRequiredAttributes = Collections.emptySet();

        if (tag instanceof HtmlTag) {
            InspectionProfile profile = InspectionProjectProfileManager.getInstance(tag.getProject()).getInspectionProfile();
            BaseXmlEntitiesInspectionState state = profile.getToolState(XmlEntitiesInspection.REQUIRED_ATTRIBUTES_SHORT_NAME, tag);

            if (state != null) {
                Collections.addAll(notRequiredAttributes, state.getEntities());
            }
        }

        XmlAttributeDescriptor[] attributes = descriptor.getAttributesDescriptors(tag);
        StringBuilder indirectRequiredAttrs = null;

        if (XmlEditorOptions.getInstance().isAutomaticallyInsertRequiredAttributes()) {
            XmlExtension extension = XmlExtension.getExtension(containingFile);

            for (XmlAttributeDescriptor attributeDecl : attributes) {
                String attributeName = attributeDecl.getName(tag);

                if (attributeDecl.isRequired() && (tag == null || tag.getAttributeValue(attributeName) == null)) {
                    if (!notRequiredAttributes.contains(attributeName)) {
                        if (!extension.isIndirectSyntax(attributeDecl)) {
                            template.addTextSegment(" " + attributeName + "=\"");
                            template.addVariable(new MacroCallNode(new CompleteMacro()), true);
                            template.addTextSegment("\"");
                        }
                        else {
                            if (indirectRequiredAttrs == null) {
                                indirectRequiredAttrs = new StringBuilder();
                            }
                            indirectRequiredAttrs.append("\n<jsp:attribute name=\"").append(attributeName).append("\"></jsp:attribute>\n");
                        }
                    }
                }
                else if (attributeDecl.isRequired() && attributeDecl.isFixed() && attributeDecl.getDefaultValue() != null && !htmlCode) {
                    template.addTextSegment(" " + attributeName + "=\"" + attributeDecl.getDefaultValue() + "\"");
                }
            }
        }
        return indirectRequiredAttrs;
    }

    protected static boolean addTail(
        char completionChar,
        XmlElementDescriptor descriptor,
        boolean isHtmlCode,
        XmlTag tag,
        Template template,
        StringBuilder indirectRequiredAttrs
    ) {
        if (completionChar == '>' || (completionChar == '/' && indirectRequiredAttrs != null)) {
            template.addTextSegment(">");
            boolean toInsertCDataEnd = false;

            if (descriptor instanceof XmlElementDescriptorWithCDataContent cDataContainer
                && cDataContainer.requiresCdataBracesInContext(tag)) {
                template.addTextSegment("<![CDATA[\n");
                toInsertCDataEnd = true;
            }

            if (indirectRequiredAttrs != null) {
                template.addTextSegment(indirectRequiredAttrs.toString());
            }
            template.addEndVariable();

            if (toInsertCDataEnd) {
                template.addTextSegment("\n]]>");
            }

            if ((!(tag instanceof HtmlTag) || !HtmlUtil.isSingleHtmlTag(tag.getName())) && tag.getAttributes().length == 0) {
                if (XmlEditorOptions.getInstance().isAutomaticallyInsertClosingTag()) {
                    String name = descriptor.getName(tag);
                    if (name != null) {
                        template.addTextSegment("</");
                        template.addTextSegment(name);
                        template.addTextSegment(">");
                    }
                }
            }
        }
        else if (completionChar == '/') {
            template.addTextSegment("/>");
        }
        else if (completionChar == ' ' && template.getSegmentsCount() == 0) {
            if (XmlEditorOptions.getInstance().isAutomaticallyStartAttribute()
                && (descriptor.getAttributesDescriptors(tag).length > 0
                || isTagFromHtml(tag) && !HtmlUtil.isTagWithoutAttributes(tag.getName()))) {
                completeAttribute(template);
                return true;
            }
        }
        else if (completionChar == Lookup.AUTO_INSERT_SELECT_CHAR
            || completionChar == Lookup.NORMAL_SELECT_CHAR
            || completionChar == Lookup.REPLACE_SELECT_CHAR) {
            if (XmlEditorOptions.getInstance().isAutomaticallyInsertClosingTag() && isHtmlCode && HtmlUtil.isSingleHtmlTag(tag.getName())) {
                template.addTextSegment(HtmlUtil.isHtmlTag(tag) ? ">" : "/>");
            }
            else if (needAlLeastOneAttribute(tag) && XmlEditorOptions.getInstance()
                .isAutomaticallyStartAttribute() && tag.getAttributes().length == 0 && template.getSegmentsCount() == 0) {
                completeAttribute(template);
                return true;
            }
            else {
                completeTagTail(template, descriptor, tag.getContainingFile(), tag, true);
            }
        }

        return false;
    }

    private static void completeAttribute(Template template) {
        template.addTextSegment(" ");
        template.addVariable(new MacroCallNode(new CompleteMacro()), true);
        template.addTextSegment("=\"");
        template.addEndVariable();
        template.addTextSegment("\"");
    }

    private static boolean needAlLeastOneAttribute(XmlTag tag) {
        for (XmlTagRuleProvider ruleProvider : XmlTagRuleProvider.EP_NAME.getExtensionList()) {
            for (XmlTagRuleProvider.Rule rule : ruleProvider.getTagRule(tag)) {
                if (rule.needAtLeastOneAttribute(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean addRequiredSubTags(Template template, XmlElementDescriptor descriptor, PsiFile file, XmlTag context) {
        if (!XmlEditorOptions.getInstance().isAutomaticallyInsertRequiredSubTags()) {
            return false;
        }
        List<XmlElementDescriptor> requiredSubTags = GenerateXmlTagAction.getRequiredSubTags(descriptor);
        if (!requiredSubTags.isEmpty()) {
            template.addTextSegment(">");
            template.setToReformat(true);
        }
        for (XmlElementDescriptor subTag : requiredSubTags) {
            if (subTag == null) { // placeholder for smart completion
                template.addTextSegment("<");
                template.addVariable(new MacroCallNode(new CompleteSmartMacro()), true);
                continue;
            }
            String qname = subTag.getName();
            if (subTag instanceof XmlElementDescriptorImpl elementDescriptor) {
                String prefixByNamespace = context.getPrefixByNamespace(elementDescriptor.getNamespace());
                if (StringUtil.isNotEmpty(prefixByNamespace)) {
                    qname = prefixByNamespace + ":" + elementDescriptor.getName();
                }
            }
            template.addTextSegment("<" + qname);
            addRequiredAttributes(subTag, null, template, file);
            completeTagTail(template, subTag, file, context, false);
        }
        if (!requiredSubTags.isEmpty()) {
            addTagEnd(template, descriptor, context);
        }
        return !requiredSubTags.isEmpty();
    }

    private static void completeTagTail(
        Template template,
        XmlElementDescriptor descriptor,
        PsiFile file,
        XmlTag context,
        boolean firstLevel
    ) {
        boolean completeIt = !firstLevel || descriptor.getAttributesDescriptors(null).length == 0;
        switch (descriptor.getContentType()) {
            case XmlElementDescriptor.CONTENT_TYPE_UNKNOWN:
                return;
            case XmlElementDescriptor.CONTENT_TYPE_EMPTY:
                if (completeIt) {
                    template.addTextSegment("/>");
                }
                break;
            case XmlElementDescriptor.CONTENT_TYPE_MIXED:
                if (completeIt) {
                    template.addTextSegment(">");
                    if (firstLevel) {
                        template.addEndVariable();
                    }
                    else {
                        template.addVariable(new MacroCallNode(new CompleteMacro()), true);
                    }
                    addTagEnd(template, descriptor, context);
                }
                break;
            default:
                if (!addRequiredSubTags(template, descriptor, file, context)) {
                    if (completeIt) {
                        template.addTextSegment(">");
                        template.addEndVariable();
                        addTagEnd(template, descriptor, context);
                    }
                }
                break;
        }
    }

    private static void addTagEnd(Template template, XmlElementDescriptor descriptor, XmlTag context) {
        template.addTextSegment("</" + descriptor.getName(context) + ">");
    }

    private static boolean isTagFromHtml(XmlTag tag) {
        String ns = tag.getNamespace();
        return XmlUtil.XHTML_URI.equals(ns) || XmlUtil.HTML_URI.equals(ns);
    }
}
