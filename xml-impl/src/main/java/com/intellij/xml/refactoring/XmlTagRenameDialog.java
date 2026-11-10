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
package com.intellij.xml.refactoring;

import consulo.disposer.Disposable;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.RefactoringDialogDescriptor;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.UsageViewUtil;
import consulo.util.lang.StringUtil;
import consulo.xml.codeInsight.completion.TagNameReferenceCompletionProvider;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.localize.XmlLocalize;
import consulo.xml.psi.impl.source.xml.TagNameReference;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * @author spleaner
 * @since 2007-08-09
 */
public class XmlTagRenameDialog extends RefactoringDialogDescriptor {
    private static final Logger LOG = Logger.getInstance(XmlTagRenameDialog.class);

    private final PsiElement myElement;
    private final XmlTag myTag;
    private String myNewName;
    private @Nullable EditorBox myNameBox;
    private boolean myNameSelected;

    public XmlTagRenameDialog(PsiElement element, XmlTag tag) {
        super(element.getProject(), RefactoringLocalize.renameTitle());

        myElement = element;
        myTag = tag;
        myNewName = tag.getName();
    }

    private static String getFullName(XmlTag tag) {
        String name = DescriptiveNameUtil.getDescriptiveName(tag);
        return (UsageViewUtil.getType(tag) + " " + name).trim();
    }

    @RequiredUIAccess
    public static void renameXmlTag(PsiElement element, XmlTag tag) {
        new XmlTagRenameDialog(element, tag).showAsync();
    }

    @Override
    protected boolean hasPreviewButton() {
        return false;
    }

    @Override
    @RequiredUIAccess
    protected Component createContentComponent(Disposable uiDisposable) {
        EditorBox nameBox = myProject.getApplication()
            .getInstance(EditorBoxBuilderFactory.class)
            .create(myProject)
            .text(myNewName)
            .completion(new TextFieldCompletionProvider() {
                @Override
                public void addCompletionVariants(String text, int offset, String prefix, CompletionResultSet result) {
                    if (myTag.getReference() instanceof TagNameReference) {
                        result.addAllElements(Arrays.asList(
                            TagNameReferenceCompletionProvider.getTagNameVariants(myTag, myTag.getNamespacePrefix())
                        ));
                    }
                }
            })
            .build();
        nameBox.addValueListener(event -> {
            String value = event.getValue();
            myNewName = value == null ? "" : value;
            validateButtons();
        });
        myNameBox = nameBox;

        TableLayout nameRow = TableLayout.create(StaticPosition.TOP);
        nameRow.add(nameBox, TableLayout.cell(0, 0).fill());

        VerticalLayout layout = VerticalLayout.create();
        layout.add(Label.create(XmlLocalize.renameCurrentTag(getFullName(myTag))));
        layout.add(nameRow);
        return layout;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedComponent() {
        EditorBox nameBox = myNameBox;
        if (nameBox != null && !myNameSelected) {
            myNameSelected = true;
            nameBox.selectAll();
        }
        return nameBox;
    }

    @Override
    @RequiredUIAccess
    protected void doAction() {
        LOG.assertTrue(myElement.isValid());

        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(RefactoringLocalize.renameTitle())
            .inWriteAction()
            .run(() -> {
                try {
                    myTag.setName(getNewName());
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
            });

        closeOKAction();
    }

    public String getNewName() {
        return myNewName.trim();
    }

    @Override
    protected boolean areButtonsValid() {
        String newName = getNewName();
        return !StringUtil.containsAnyChar(
            newName,
            "\t ;*'\"\\/,()^&<>={}"
        ); // RenameUtil.isValidName(myProject, myTag, newName); // IDEADEV-34531
    }
}
