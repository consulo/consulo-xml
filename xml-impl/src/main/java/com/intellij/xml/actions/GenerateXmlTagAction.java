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
package com.intellij.xml.actions;

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.impl.schema.XmlElementDescriptorImpl;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorFontType;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.CodeInsightUtilCore;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.action.SimpleCodeInsightAction;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.template.TemplateBuilder;
import consulo.language.editor.template.TemplateBuilderFactory;
import consulo.language.editor.template.macro.CompleteMacro;
import consulo.language.editor.template.macro.CompleteSmartMacro;
import consulo.language.editor.template.macro.MacroCallNode;
import consulo.language.editor.util.LanguageEditorUtil;
import consulo.language.impl.ast.Factory;
import consulo.language.impl.ast.LeafElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.impl.source.xml.XmlContentDFA;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class GenerateXmlTagAction extends SimpleCodeInsightAction {
    private final static Logger LOG = Logger.getInstance(GenerateXmlTagAction.class);

    @RequiredUIAccess
    @Override
    public void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull final PsiFile file) {
        if (!LanguageEditorUtil.checkModificationAllowed(editor)) {
            return;
        }
        try {
            final XmlTag contextTag = getContextTag(editor, file);
            if (contextTag == null) {
                throw new CommonRefactoringUtil.RefactoringErrorHintException("Caret should be positioned inside a tag");
            }
            XmlElementDescriptor currentTagDescriptor = contextTag.getDescriptor();
            final XmlElementDescriptor[] descriptors = currentTagDescriptor.getElementsDescriptors(contextTag);
            Arrays.sort(descriptors, (o1, o2) -> o1.getName().compareTo(o2.getName()));

            JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(List.of(descriptors))
                .setTitle("Choose Tag Name")
                .setRenderer(new ColoredListCellRenderer<XmlElementDescriptor>() {
                    @Override
                    protected void customizeCellRenderer(
                        @Nonnull JList<? extends XmlElementDescriptor> list,
                        XmlElementDescriptor value,
                        int index,
                        boolean selected,
                        boolean hasFocus
                    ) {
                        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
                        Font font = scheme.getFont(EditorFontType.PLAIN);

                        setFont(font);
                        append(value.getName());
                        String namespace = getNamespace(value);
                        if (!namespace.isEmpty()) {
                            append(namespace, SimpleTextAttributes.GRAY_ATTRIBUTES);
                        }
                    }
                })
                .setItemChosenCallback(selected -> {
                    if (selected == null) {
                        return;
                    }

                    new WriteCommandAction.Simple(project, "Generate XML Tag", file) {
                        @Override
                        @RequiredWriteAction
                        protected void run() {
                            XmlTag newTag = createTag(contextTag, selected);

                            PsiElement anchor = getAnchor(contextTag, editor, selected);
                            if (anchor == null) { // insert it in the cursor position
                                int offset = editor.getCaretModel().getOffset();
                                Document document = editor.getDocument();
                                document.insertString(offset, newTag.getText());
                                PsiDocumentManager.getInstance(project).commitDocument(document);
                                newTag = PsiTreeUtil.getParentOfType(file.findElementAt(offset + 1), XmlTag.class, false);
                            }
                            else {
                                newTag = (XmlTag)contextTag.addAfter(newTag, anchor);
                            }
                            generateTag(newTag);
                        }
                    }.execute();
                })
                .setNamerForFiltering(PsiMetaData::getName)
                .createPopup();

            editor.showPopupInBestPositionFor(popup);
        }
        catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
            HintManager.getInstance().showErrorHint(editor, e.getMessage());
        }
    }

    @Nullable
    private static XmlTag getAnchor(@Nonnull XmlTag contextTag, Editor editor, XmlElementDescriptor selected) {
        XmlContentDFA contentDFA = XmlContentDFA.getContentDFA(contextTag);
        int offset = editor.getCaretModel().getOffset();
        if (contentDFA == null) {
            return null;
        }
        XmlTag anchor = null;
        boolean previousPositionIsPossible = true;
        for (XmlTag subTag : contextTag.getSubTags()) {
            if (contentDFA.getPossibleElements().contains(selected)) {
                if (subTag.getTextOffset() > offset) {
                    break;
                }
                anchor = subTag;
                previousPositionIsPossible = true;
            }
            else {
                previousPositionIsPossible = false;
            }
            contentDFA.transition(subTag);
        }
        return previousPositionIsPossible ? null : anchor;
    }

    @RequiredReadAction
    public static void generateTag(XmlTag newTag) {
        generateRaw(newTag);
        final XmlTag restored = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(newTag);
        if (restored == null) {
            LOG.error("Could not restore tag: " + newTag.getText());
        }
        TemplateBuilder builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(restored);
        replaceElements(restored, builder);
        builder.run();
    }

    @RequiredReadAction
    private static void generateRaw(final XmlTag newTag) {
        XmlElementDescriptor selected = newTag.getDescriptor();
        if (selected == null) {
            return;
        }
        switch (selected.getContentType()) {
            case XmlElementDescriptor.CONTENT_TYPE_EMPTY:
                newTag.collapseIfEmpty();
                ASTNode node = newTag.getNode();
                assert node != null;
                ASTNode elementEnd = node.findChildByType(XmlTokenType.XML_EMPTY_ELEMENT_END);
                if (elementEnd == null) {
                    LeafElement emptyTagEnd = Factory.createSingleLeafElement(
                        XmlTokenType.XML_EMPTY_ELEMENT_END,
                        "/>",
                        0,
                        2,
                        null,
                        newTag.getManager()
                    );
                    node.addChild(emptyTagEnd);
                }
                break;
            case XmlElementDescriptor.CONTENT_TYPE_MIXED:
                newTag.getValue().setText("");
        }
        for (XmlAttributeDescriptor descriptor : selected.getAttributesDescriptors(newTag)) {
            if (descriptor.isRequired()) {
                newTag.setAttribute(descriptor.getName(), "");
            }
        }
        List<XmlElementDescriptor> tags = getRequiredSubTags(selected);
        for (XmlElementDescriptor descriptor : tags) {
            if (descriptor == null) {
                XmlTag tag = XmlElementFactory.getInstance(newTag.getProject()).createTagFromText("<", newTag.getLanguage());
                newTag.addSubTag(tag, false);
            }
            else {
                XmlTag subTag = newTag.addSubTag(createTag(newTag, descriptor), false);
                generateRaw(subTag);
            }
        }
    }

    public static List<XmlElementDescriptor> getRequiredSubTags(XmlElementDescriptor selected) {
        XmlElementsGroup topGroup = selected.getTopGroup();
        if (topGroup == null) {
            return Collections.emptyList();
        }
        return computeRequiredSubTags(topGroup);
    }

    @RequiredReadAction
    private static void replaceElements(XmlTag tag, TemplateBuilder builder) {
        for (XmlAttribute attribute : tag.getAttributes()) {
            XmlAttributeValue value = attribute.getValueElement();
            if (value != null) {
                builder.replaceElement(value, TextRange.from(1, 0), new MacroCallNode(new CompleteMacro()));
            }
        }
        if ("<".equals(tag.getText())) {
            builder.replaceElement(tag, TextRange.from(1, 0), new MacroCallNode(new CompleteSmartMacro()));
        }
        else if (tag.getSubTags().length == 0) {
            int i = tag.getText().indexOf("></");
            if (i > 0) {
                builder.replaceElement(tag, TextRange.from(i + 1, 0), new MacroCallNode(new CompleteMacro()));
            }
        }
        for (XmlTag subTag : tag.getSubTags()) {
            replaceElements(subTag, builder);
        }
    }

    @RequiredReadAction
    private static XmlTag createTag(@Nonnull XmlTag contextTag, @Nonnull XmlElementDescriptor descriptor) {
        String namespace = getNamespace(descriptor);
        XmlTag tag = contextTag.createChildTag(descriptor.getName(), namespace, null, false);
        PsiElement lastChild = tag.getLastChild();
        assert lastChild != null;
        lastChild.delete(); // remove XML_EMPTY_ELEMENT_END
        return tag;
    }

    private static String getNamespace(XmlElementDescriptor descriptor) {
        return descriptor instanceof XmlElementDescriptorImpl ? ((XmlElementDescriptorImpl)descriptor).getNamespace() : "";
    }

    @RequiredReadAction
    @Nullable
    private static XmlTag getContextTag(Editor editor, PsiFile file) {
        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        XmlTag tag = null;
        if (element != null) {
            tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        }
        if (tag == null) {
            tag = ((XmlFile)file).getRootTag();
        }
        return tag;
    }

    private static List<XmlElementDescriptor> computeRequiredSubTags(XmlElementsGroup group) {

        if (group.getMinOccurs() < 1) {
            return Collections.emptyList();
        }
        switch (group.getGroupType()) {
            case LEAF:
                XmlElementDescriptor descriptor = group.getLeafDescriptor();
                return descriptor == null ? Collections.<XmlElementDescriptor>emptyList() : Collections.singletonList(descriptor);
            case CHOICE:
                LinkedHashSet<XmlElementDescriptor> set = null;
                for (XmlElementsGroup subGroup : group.getSubGroups()) {
                    List<XmlElementDescriptor> descriptors = computeRequiredSubTags(subGroup);
                    if (set == null) {
                        set = new LinkedHashSet<>(descriptors);
                    }
                    else {
                        set.retainAll(descriptors);
                    }
                }
                if (set == null || set.isEmpty()) {
                    return Collections.singletonList(null); // placeholder for smart completion
                }
                return new ArrayList<>(set);

            default:
                ArrayList<XmlElementDescriptor> list = new ArrayList<>();
                for (XmlElementsGroup subGroup : group.getSubGroups()) {
                    list.addAll(computeRequiredSubTags(subGroup));
                }
                return list;
        }
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        if (!(file instanceof XmlFile)) {
            return false;
        }
        XmlTag contextTag = getContextTag(editor, file);
        return contextTag != null && contextTag.getDescriptor() != null;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
