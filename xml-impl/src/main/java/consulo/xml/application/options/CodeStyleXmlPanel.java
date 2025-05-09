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

import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.ide.highlighter.XmlHighlighterFactory;
import consulo.xml.psi.formatter.xml.XmlCodeStyleSettings;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

public class CodeStyleXmlPanel extends CodeStyleAbstractPanel {
    private JTextField myKeepBlankLines;
    private JComboBox myWrapAttributes;
    private JCheckBox myAlignAttributes;
    private JCheckBox myKeepWhiteSpaces;

    private JPanel myPanel;
    private JPanel myPreviewPanel;

    private JCheckBox mySpacesAroundEquality;
    private JCheckBox mySpacesAfterTagName;
    private JCheckBox myKeepLineBreaks;
    private JCheckBox myInEmptyTag;
    private JCheckBox myWrapText;
    private JCheckBox myKeepLineBreaksInText;
    private JComboBox myWhiteSpaceAroundCDATA;
    private JCheckBox myKeepWhitespaceInsideCDATACheckBox;
    private JBScrollPane myJBScrollPane;

    public CodeStyleXmlPanel(CodeStyleSettings settings) {
        super(settings);
        installPreviewPanel(myPreviewPanel);

        fillWrappingCombo(myWrapAttributes);

        addPanelToWatch(myPanel);
    }

    @Override
    protected EditorHighlighter createHighlighter(final EditorColorsScheme scheme) {
        return XmlHighlighterFactory.createXMLHighlighter(scheme);
    }

    @Override
    protected int getRightMargin() {
        return 60;
    }

    public void apply(CodeStyleSettings settings) {
        XmlCodeStyleSettings xmlSettings = settings.getCustomSettings(XmlCodeStyleSettings.class);
        xmlSettings.XML_KEEP_BLANK_LINES = getIntValue(myKeepBlankLines);
        xmlSettings.XML_KEEP_LINE_BREAKS = myKeepLineBreaks.isSelected();
        xmlSettings.XML_KEEP_LINE_BREAKS_IN_TEXT = myKeepLineBreaksInText.isSelected();
        xmlSettings.XML_ATTRIBUTE_WRAP = ourWrappings[myWrapAttributes.getSelectedIndex()];
        xmlSettings.XML_TEXT_WRAP = myWrapText.isSelected() ? CodeStyleSettings.WRAP_AS_NEEDED : CodeStyleSettings.DO_NOT_WRAP;
        xmlSettings.XML_ALIGN_ATTRIBUTES = myAlignAttributes.isSelected();
        xmlSettings.XML_KEEP_WHITESPACES = myKeepWhiteSpaces.isSelected();
        xmlSettings.XML_SPACE_AROUND_EQUALITY_IN_ATTRIBUTE = mySpacesAroundEquality.isSelected();
        xmlSettings.XML_SPACE_AFTER_TAG_NAME = mySpacesAfterTagName.isSelected();
        xmlSettings.XML_SPACE_INSIDE_EMPTY_TAG = myInEmptyTag.isSelected();
        xmlSettings.XML_WHITE_SPACE_AROUND_CDATA = myWhiteSpaceAroundCDATA.getSelectedIndex();
        xmlSettings.XML_KEEP_WHITE_SPACES_INSIDE_CDATA = myKeepWhitespaceInsideCDATACheckBox.isSelected();
    }

    private int getIntValue(JTextField keepBlankLines) {
        try {
            return Integer.parseInt(keepBlankLines.getText());
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void resetImpl(final CodeStyleSettings settings) {
        XmlCodeStyleSettings xmlSettings = settings.getCustomSettings(XmlCodeStyleSettings.class);
        myKeepBlankLines.setText(String.valueOf(xmlSettings.XML_KEEP_BLANK_LINES));
        myWrapAttributes.setSelectedIndex(getIndexForWrapping(xmlSettings.XML_ATTRIBUTE_WRAP));
        myAlignAttributes.setSelected(xmlSettings.XML_ALIGN_ATTRIBUTES);
        myKeepWhiteSpaces.setSelected(xmlSettings.XML_KEEP_WHITESPACES);
        mySpacesAfterTagName.setSelected(xmlSettings.XML_SPACE_AFTER_TAG_NAME);
        mySpacesAroundEquality.setSelected(xmlSettings.XML_SPACE_AROUND_EQUALITY_IN_ATTRIBUTE);
        myKeepLineBreaks.setSelected(xmlSettings.XML_KEEP_LINE_BREAKS);
        myKeepLineBreaksInText.setSelected(xmlSettings.XML_KEEP_LINE_BREAKS_IN_TEXT);
        myInEmptyTag.setSelected(xmlSettings.XML_SPACE_INSIDE_EMPTY_TAG);
        myWrapText.setSelected(wrapText(settings));
        myWhiteSpaceAroundCDATA.setSelectedIndex(xmlSettings.XML_WHITE_SPACE_AROUND_CDATA);
        myKeepWhitespaceInsideCDATACheckBox.setSelected(xmlSettings.XML_KEEP_WHITE_SPACES_INSIDE_CDATA);
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        XmlCodeStyleSettings xmlSettings = settings.getCustomSettings(XmlCodeStyleSettings.class);
        return myWrapText.isSelected() != wrapText(settings)
            || xmlSettings.XML_KEEP_BLANK_LINES != getIntValue(myKeepBlankLines)
            || xmlSettings.XML_ATTRIBUTE_WRAP != ourWrappings[myWrapAttributes.getSelectedIndex()]
            || xmlSettings.XML_ALIGN_ATTRIBUTES != myAlignAttributes.isSelected()
            || xmlSettings.XML_KEEP_WHITESPACES != myKeepWhiteSpaces.isSelected()
            || xmlSettings.XML_SPACE_AROUND_EQUALITY_IN_ATTRIBUTE != mySpacesAroundEquality.isSelected()
            || xmlSettings.XML_SPACE_AFTER_TAG_NAME != mySpacesAfterTagName.isSelected()
            || xmlSettings.XML_KEEP_LINE_BREAKS != myKeepLineBreaks.isSelected()
            || xmlSettings.XML_KEEP_LINE_BREAKS_IN_TEXT != myKeepLineBreaksInText.isSelected()
            || xmlSettings.XML_SPACE_INSIDE_EMPTY_TAG != myInEmptyTag.isSelected()
            || xmlSettings.XML_WHITE_SPACE_AROUND_CDATA != myWhiteSpaceAroundCDATA.getSelectedIndex()
            || xmlSettings.XML_KEEP_WHITE_SPACES_INSIDE_CDATA != this.myKeepWhitespaceInsideCDATACheckBox.isSelected();

    }

    private boolean wrapText(final CodeStyleSettings settings) {
        XmlCodeStyleSettings xmlSettings = settings.getCustomSettings(XmlCodeStyleSettings.class);
        return xmlSettings.XML_TEXT_WRAP == CodeStyleSettings.WRAP_AS_NEEDED;
    }

    @Override
    public JComponent getPanel() {
        return myPanel;
    }

    @Override
    protected String getPreviewText() {
        return readFromFile(getClass(), "preview.xml.template");
    }

    @Nonnull
    @Override
    protected FileType getFileType() {
        return XmlFileType.INSTANCE;
    }

    @Override
    protected void prepareForReformat(final PsiFile psiFile) {
        //psiFile.putUserData(PsiUtil.FILE_LANGUAGE_LEVEL_KEY, LanguageLevel.HIGHEST);
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
}
