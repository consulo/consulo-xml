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

/*
 * Created by IntelliJ IDEA.
 * User: spleaner
 * Date: Aug 9, 2007
 * Time: 4:45:40 PM
 */
package com.intellij.xml.refactoring;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import javax.annotation.Nullable;

import consulo.xml.codeInsight.completion.TagNameReferenceCompletionProvider;
import consulo.application.ApplicationManager;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.refactoring.ui.NameSuggestionsField;
import consulo.language.editor.refactoring.ui.RefactoringDialog;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.PsiReference;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.undoRedo.CommandProcessor;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.language.plain.PlainTextFileType;
import consulo.application.HelpManager;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.impl.source.xml.TagNameReference;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.util.IncorrectOperationException;
import com.intellij.xml.XmlBundle;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.usage.UsageViewUtil;
import consulo.util.lang.StringUtil;

public class XmlTagRenameDialog extends RefactoringDialog {
    private static final Logger LOG = Logger.getInstance(XmlTagRenameDialog.class);
    private static final String REFACTORING_NAME = RefactoringBundle.message("rename.title");

    private final PsiElement myElement;
    private final Editor myEditor;
    private JLabel myTitleLabel;
    private NameSuggestionsField myNameSuggestionsField;
    private String myHelpID;
    private final XmlTag myTag;
    private NameSuggestionsField.DataChanged myNameChangedListener;

    public XmlTagRenameDialog(@Nonnull final Editor editor, @Nonnull final PsiElement element, @Nonnull final XmlTag tag) {
        super(element.getProject(), true);

        myEditor = editor;
        myElement = element;
        myTag = tag;

        setTitle(REFACTORING_NAME);
        createNewNameComponent();

        init();

        myTitleLabel.setText(XmlBundle.message("rename.current.tag", getFullName(tag)));

        validateButtons();
    }

    @Override
    protected void dispose() {
        myNameSuggestionsField.removeDataChangedListener(myNameChangedListener);
        super.dispose();
    }

    @Override
    protected boolean hasHelpAction() {
        return false;
    }

    private static String getFullName(@Nonnull final XmlTag tag) {
        final String name = DescriptiveNameUtil.getDescriptiveName(tag);
        return (UsageViewUtil.getType(tag) + " " + name).trim();
    }

    public static void renameXmlTag(final Editor editor, @Nonnull final PsiElement element, @Nonnull final XmlTag tag) {
        final XmlTagRenameDialog dialog = new XmlTagRenameDialog(editor, element, tag);
        dialog.show();
    }

    private void createNewNameComponent() {
        myNameSuggestionsField = new NameSuggestionsField(new String[]{myTag.getName()}, myProject, PlainTextFileType.INSTANCE, myEditor);
        myNameChangedListener = new NameSuggestionsField.DataChanged() {
            @Override
            public void dataChanged() {
                validateButtons();
            }
        };
        myNameSuggestionsField.addDataChangedListener(myNameChangedListener);

        myNameSuggestionsField.getComponent().registerKeyboardAction(
            e -> completeVariable(myNameSuggestionsField.getEditor()),
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void completeVariable(final Editor editor) {
        String prefix = myNameSuggestionsField.getEnteredName();

        final PsiReference reference = myTag.getReference();
        if (reference instanceof TagNameReference) {
            LookupElement[] lookupItems = TagNameReferenceCompletionProvider.getTagNameVariants(myTag, myTag.getNamespacePrefix());
            editor.getCaretModel().moveToOffset(prefix.length());
            editor.getSelectionModel().removeSelection();
            LookupManager.getInstance(getProject()).showLookup(editor, lookupItems, prefix);
        }
    }

    @Override
    protected void doAction() {
        LOG.assertTrue(myElement.isValid());

        CommandProcessor.getInstance().executeCommand(
            myProject,
            () -> ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    myTag.setName(getNewName());
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
            }),
            RefactoringBundle.message("rename.title"),
            null
        );

        close(DialogWrapper.OK_EXIT_CODE);
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return null;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myNameSuggestionsField.getFocusableComponent();
    }

    @Override
    protected JComponent createNorthPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        myTitleLabel = new JLabel();
        panel.add(myTitleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(myNameSuggestionsField.getComponent());

        return panel;
    }

    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(myHelpID);
    }

    public String getNewName() {
        return myNameSuggestionsField.getEnteredName().trim();
    }

    @Override
    protected void validateButtons() {
        super.validateButtons();

        getPreviewAction().setEnabled(false);
    }

    @Override
    protected boolean areButtonsValid() {
        final String newName = getNewName();
        return !StringUtil.containsAnyChar(
            newName,
            "\t ;*'\"\\/,()^&<>={}"
        ); // RenameUtil.isValidName(myProject, myTag, newName); // IDEADEV-34531
    }
}
