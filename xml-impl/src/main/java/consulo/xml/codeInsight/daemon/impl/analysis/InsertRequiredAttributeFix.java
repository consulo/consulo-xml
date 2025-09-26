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
package consulo.xml.codeInsight.daemon.impl.analysis;

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.util.HtmlUtil;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.language.ast.ASTNode;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.editor.intention.HighPriorityAction;
import consulo.language.editor.template.*;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2005-11-18
 */
public class InsertRequiredAttributeFix extends LocalQuickFixAndIntentionActionOnPsiElement implements HighPriorityAction {
    private final String myAttrName;
    private final String[] myValues;
    private static final String NAME_TEMPLATE_VARIABLE = "name";

    public InsertRequiredAttributeFix(@Nonnull XmlTag tag, @Nonnull String attrName, @Nonnull String... values) {
        super(tag);
        myAttrName = attrName;
        myValues = values;
    }

    @Nonnull
    @Override
    public String getText() {
        return XmlErrorLocalize.insertRequiredAttributeQuickfixText(myAttrName).get();
    }

    @Nonnull
    @Override
    public String getFamilyName() {
        return XmlErrorLocalize.insertRequiredAttributeQuickfixFamily().get();
    }

    @Override
    public void invoke(
        @Nonnull Project project,
        @Nonnull PsiFile file,
        @Nullable Editor editor,
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement
    ) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        XmlTag myTag = (XmlTag)startElement;
        ASTNode treeElement = SourceTreeToPsiMap.psiElementToTree(myTag);

        XmlElementDescriptor descriptor = myTag.getDescriptor();
        if (descriptor == null) {
            return;
        }
        XmlAttributeDescriptor attrDescriptor = descriptor.getAttributeDescriptor(myAttrName, myTag);
        boolean indirectSyntax = XmlExtension.getExtension(myTag.getContainingFile()).isIndirectSyntax(attrDescriptor);
        boolean insertShorthand = myTag instanceof HtmlTag && attrDescriptor != null && HtmlUtil.isBooleanAttribute(attrDescriptor, myTag);

        PsiElement anchor = SourceTreeToPsiMap.treeElementToPsi(XmlChildRole.EMPTY_TAG_END_FINDER.findChild(treeElement));

        boolean anchorIsEmptyTag = anchor != null;

        if (anchor == null) {
            anchor = SourceTreeToPsiMap.treeElementToPsi(XmlChildRole.START_TAG_END_FINDER.findChild(treeElement));
        }

        if (anchor == null) {
            return;
        }

        Template template = TemplateManager.getInstance(project).createTemplate("", "");
        if (indirectSyntax) {
            if (anchorIsEmptyTag) {
                template.addTextSegment(">");
            }
            template.addTextSegment("<jsp:attribute name=\"" + myAttrName + "\">");
        }
        else {
            template.addTextSegment(" " + myAttrName + (!insertShorthand ? "=\"" : ""));
        }

        Expression expression = new Expression() {
            TextResult result = new TextResult("");

            @Override
            public Result calculateResult(ExpressionContext context) {
                return result;
            }

            @Override
            public Result calculateQuickResult(ExpressionContext context) {
                return null;
            }

            @Override
            public LookupElement[] calculateLookupItems(ExpressionContext context) {
                LookupElement[] items = new LookupElement[myValues.length];

                for (int i = 0; i < items.length; i++) {
                    items[i] = LookupElementBuilder.create(myValues[i]);
                }
                return items;
            }
        };
        if (!insertShorthand) {
            template.addVariable(NAME_TEMPLATE_VARIABLE, expression, expression, true);
        }

        if (indirectSyntax) {
            template.addTextSegment("</jsp:attribute>");
            template.addEndVariable();
            if (anchorIsEmptyTag) {
                template.addTextSegment("</" + myTag.getName() + ">");
            }
        }
        else if (!insertShorthand) {
            template.addTextSegment("\"");
        }

        PsiElement anchor1 = anchor;

        Runnable runnable = new Runnable() {
            @Override
            @RequiredWriteAction
            public void run() {
                Application.get().runWriteAction(() -> {
                    int textOffset = anchor1.getTextOffset();
                    if (!anchorIsEmptyTag && indirectSyntax) {
                        ++textOffset;
                    }
                    editor.getCaretModel().moveToOffset(textOffset);
                    if (anchorIsEmptyTag && indirectSyntax) {
                        editor.getDocument().deleteString(textOffset, textOffset + 2);
                    }
                    TemplateManager.getInstance(project).startTemplate(editor, template);
                });
            }
        };

        if (!Application.get().isUnitTestMode()) {
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(LocalizeValue.ofNullable(getText()))
                .groupId(getFamilyName())
                .inLater()
                .run(runnable);
        }
        else {
            runnable.run();
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
