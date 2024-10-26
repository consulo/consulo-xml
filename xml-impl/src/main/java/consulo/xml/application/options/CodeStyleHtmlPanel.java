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
package consulo.xml.application.options;

import consulo.application.AllIcons;
import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.ide.highlighter.XmlHighlighterFactory;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class CodeStyleHtmlPanel extends CodeStyleAbstractPanel {
    private JTextField myKeepBlankLines;
    private JComboBox myWrapAttributes;
    private JCheckBox myAlignAttributes;
    private JCheckBox myKeepWhiteSpaces;

    private JPanel myPanel;
    private JPanel myPreviewPanel;

    private JCheckBox mySpacesAroundEquality;
    private JCheckBox mySpacesAroundTagName;
    private JCheckBox myAlignText;
    private TextFieldWithBrowseButton myInsertNewLineTagNames;
    private TextFieldWithBrowseButton myRemoveNewLineTagNames;
    private TextFieldWithBrowseButton myDoNotAlignChildrenTagNames;
    private TextFieldWithBrowseButton myKeepWhiteSpacesTagNames;
    private TextFieldWithBrowseButton myInlineElementsTagNames;
    private JTextField myDoNotAlignChildrenMinSize;
    private JCheckBox myShouldKeepBlankLines;
    private JCheckBox mySpaceInEmptyTag;
    private JCheckBox myWrapText;
    private JCheckBox myShouldKeepLineBreaksInText;
    private TextFieldWithBrowseButton myDontBreakIfInlineContent;
    private JBScrollPane myJBScrollPane;

    public CodeStyleHtmlPanel(CodeStyleSettings settings) {
        super(settings);
        installPreviewPanel(myPreviewPanel);

        fillWrappingCombo(myWrapAttributes);

        customizeField(ApplicationLocalize.titleInsertNewLineBeforeTags(), myInsertNewLineTagNames);
        customizeField(ApplicationLocalize.titleRemoveLineBreaksBeforeTags(), myRemoveNewLineTagNames);
        customizeField(ApplicationLocalize.titleDoNotIndentChildrenOf(), myDoNotAlignChildrenTagNames);
        customizeField(ApplicationLocalize.titleInlineElements(), myInlineElementsTagNames);
        customizeField(ApplicationLocalize.titleKeepWhitespacesInside(), myKeepWhiteSpacesTagNames);
        customizeField(ApplicationLocalize.titleDontWrapIfInlineContent(), myDontBreakIfInlineContent);

        myInsertNewLineTagNames.getTextField().setColumns(5);
        myRemoveNewLineTagNames.getTextField().setColumns(5);
        myDoNotAlignChildrenTagNames.getTextField().setColumns(5);
        myKeepWhiteSpacesTagNames.getTextField().setColumns(5);
        myInlineElementsTagNames.getTextField().setColumns(5);
        myDontBreakIfInlineContent.getTextField().setColumns(5);

        addPanelToWatch(myPanel);
    }

    @Override
    protected EditorHighlighter createHighlighter(final EditorColorsScheme scheme) {
        return XmlHighlighterFactory.createXMLHighlighter(scheme);
    }

    private void createUIComponents() {
        myJBScrollPane = new JBScrollPane() {
            @Override
            public Dimension getPreferredSize() {
                Dimension prefSize = super.getPreferredSize();
                return new Dimension(prefSize.width + 15, prefSize.height);
            }
        };
    }

    private static void customizeField(@Nonnull final LocalizeValue title, final TextFieldWithBrowseButton uiField) {
        uiField.getTextField().setEditable(false);
        uiField.setButtonIcon(AllIcons.Actions.ShowViewer);
        uiField.addActionListener(new ActionListener() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                final TagListDialog tagListDialog = new TagListDialog(title.get());
                tagListDialog.setData(createCollectionOn(uiField.getText()));
                tagListDialog.show();
                if (tagListDialog.isOK()) {
                    uiField.setText(createStringOn(tagListDialog.getData()));
                }
            }

            private String createStringOn(final ArrayList<String> data) {
                return StringUtil.join(ArrayUtil.toStringArray(data), ",");
            }

            private ArrayList<String> createCollectionOn(final String data) {
                if (data == null) {
                    return new ArrayList<>();
                }
                return new ArrayList<>(Arrays.asList(data.split(",")));
            }
        });
    }

    @Override
    protected int getRightMargin() {
        return 60;
    }

    @Override
    public void apply(CodeStyleSettings settings) {
        settings.HTML_KEEP_BLANK_LINES = getIntValue(myKeepBlankLines);
        settings.HTML_ATTRIBUTE_WRAP = ourWrappings[myWrapAttributes.getSelectedIndex()];
        settings.HTML_TEXT_WRAP = myWrapText.isSelected() ? CodeStyleSettings.WRAP_AS_NEEDED : CodeStyleSettings.DO_NOT_WRAP;
        settings.HTML_SPACE_INSIDE_EMPTY_TAG = mySpaceInEmptyTag.isSelected();
        settings.HTML_ALIGN_ATTRIBUTES = myAlignAttributes.isSelected();
        settings.HTML_ALIGN_TEXT = myAlignText.isSelected();
        settings.HTML_KEEP_WHITESPACES = myKeepWhiteSpaces.isSelected();
        settings.HTML_SPACE_AROUND_EQUALITY_IN_ATTRINUTE = mySpacesAroundEquality.isSelected();
        settings.HTML_SPACE_AFTER_TAG_NAME = mySpacesAroundTagName.isSelected();

        settings.HTML_ELEMENTS_TO_INSERT_NEW_LINE_BEFORE = myInsertNewLineTagNames.getText();
        settings.HTML_ELEMENTS_TO_REMOVE_NEW_LINE_BEFORE = myRemoveNewLineTagNames.getText();
        settings.HTML_DO_NOT_INDENT_CHILDREN_OF = myDoNotAlignChildrenTagNames.getText();
        settings.HTML_DO_NOT_ALIGN_CHILDREN_OF_MIN_LINES = getIntValue(myDoNotAlignChildrenMinSize);
        settings.HTML_INLINE_ELEMENTS = myInlineElementsTagNames.getText();
        settings.HTML_DONT_ADD_BREAKS_IF_INLINE_CONTENT = myDontBreakIfInlineContent.getText();
        settings.HTML_KEEP_WHITESPACES_INSIDE = myKeepWhiteSpacesTagNames.getText();
        settings.HTML_KEEP_LINE_BREAKS = myShouldKeepBlankLines.isSelected();
        settings.HTML_KEEP_LINE_BREAKS_IN_TEXT = myShouldKeepLineBreaksInText.isSelected();
    }

    private static int getIntValue(JTextField keepBlankLines) {
        try {
            return Integer.parseInt(keepBlankLines.getText());
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void resetImpl(final CodeStyleSettings settings) {
        myKeepBlankLines.setText(String.valueOf(settings.HTML_KEEP_BLANK_LINES));
        myWrapAttributes.setSelectedIndex(getIndexForWrapping(settings.HTML_ATTRIBUTE_WRAP));
        myWrapText.setSelected(settings.HTML_TEXT_WRAP != CodeStyleSettings.DO_NOT_WRAP);
        mySpaceInEmptyTag.setSelected(settings.HTML_SPACE_INSIDE_EMPTY_TAG);
        myAlignAttributes.setSelected(settings.HTML_ALIGN_ATTRIBUTES);
        myAlignText.setSelected(settings.HTML_ALIGN_TEXT);
        myKeepWhiteSpaces.setSelected(settings.HTML_KEEP_WHITESPACES);
        mySpacesAroundTagName.setSelected(settings.HTML_SPACE_AFTER_TAG_NAME);
        mySpacesAroundEquality.setSelected(settings.HTML_SPACE_AROUND_EQUALITY_IN_ATTRINUTE);
        myShouldKeepBlankLines.setSelected(settings.HTML_KEEP_LINE_BREAKS);
        myShouldKeepLineBreaksInText.setSelected(settings.HTML_KEEP_LINE_BREAKS_IN_TEXT);

        myInsertNewLineTagNames.setText(settings.HTML_ELEMENTS_TO_INSERT_NEW_LINE_BEFORE);
        myRemoveNewLineTagNames.setText(settings.HTML_ELEMENTS_TO_REMOVE_NEW_LINE_BEFORE);
        myDoNotAlignChildrenTagNames.setText(settings.HTML_DO_NOT_INDENT_CHILDREN_OF);
        myDoNotAlignChildrenMinSize.setText(String.valueOf(settings.HTML_DO_NOT_ALIGN_CHILDREN_OF_MIN_LINES));
        myInlineElementsTagNames.setText(settings.HTML_INLINE_ELEMENTS);
        myDontBreakIfInlineContent.setText(settings.HTML_DONT_ADD_BREAKS_IF_INLINE_CONTENT);
        myKeepWhiteSpacesTagNames.setText(settings.HTML_KEEP_WHITESPACES_INSIDE);
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        return settings.HTML_KEEP_BLANK_LINES != getIntValue(myKeepBlankLines)
            || settings.HTML_ATTRIBUTE_WRAP != ourWrappings[myWrapAttributes.getSelectedIndex()]
            || (settings.HTML_TEXT_WRAP == CodeStyleSettings.WRAP_AS_NEEDED) != myWrapText.isSelected()
            || settings.HTML_SPACE_INSIDE_EMPTY_TAG != mySpaceInEmptyTag.isSelected()
            || settings.HTML_ALIGN_ATTRIBUTES != myAlignAttributes.isSelected()
            || settings.HTML_ALIGN_TEXT != myAlignText.isSelected()
            || settings.HTML_KEEP_WHITESPACES != myKeepWhiteSpaces.isSelected()
            || settings.HTML_SPACE_AROUND_EQUALITY_IN_ATTRINUTE != mySpacesAroundEquality.isSelected()
            || settings.HTML_SPACE_AFTER_TAG_NAME != mySpacesAroundTagName.isSelected()
            || !Objects.equals(settings.HTML_ELEMENTS_TO_INSERT_NEW_LINE_BEFORE, myInsertNewLineTagNames.getText().trim())
            || !Objects.equals(settings.HTML_ELEMENTS_TO_REMOVE_NEW_LINE_BEFORE, myRemoveNewLineTagNames.getText().trim())
            || !Objects.equals(settings.HTML_DO_NOT_INDENT_CHILDREN_OF, myDoNotAlignChildrenTagNames.getText().trim())
            || settings.HTML_DO_NOT_ALIGN_CHILDREN_OF_MIN_LINES != getIntValue(myDoNotAlignChildrenMinSize)
            || !Comparing.equal(settings.HTML_INLINE_ELEMENTS, myInlineElementsTagNames.getText().trim())
            || !Objects.equals(settings.HTML_DONT_ADD_BREAKS_IF_INLINE_CONTENT, myDontBreakIfInlineContent.getText().trim())
            || !Objects.equals(settings.HTML_KEEP_WHITESPACES_INSIDE, myKeepWhiteSpacesTagNames.getText().trim())
            || myShouldKeepBlankLines.isSelected() != settings.HTML_KEEP_LINE_BREAKS
            || myShouldKeepLineBreaksInText.isSelected() != settings.HTML_KEEP_LINE_BREAKS_IN_TEXT;
    }

    @Override
    public JComponent getPanel() {
        return myPanel;
    }

    @Override
    protected String getPreviewText() {
        return readFromFile(this.getClass(), "preview.html.template");
    }

    @Nonnull
    @Override
    protected FileType getFileType() {
        return HtmlFileType.INSTANCE;
    }

    @Override
    protected void prepareForReformat(final PsiFile psiFile) {
        //psiFile.putUserData(PsiUtil.FILE_LANGUAGE_LEVEL_KEY, LanguageLevel.HIGHEST);
    }
}
