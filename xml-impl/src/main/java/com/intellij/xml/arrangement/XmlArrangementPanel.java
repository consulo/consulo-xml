package com.intellij.xml.arrangement;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.ui.setting.arrangement.ArrangementSettingsPanel;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.language.XMLLanguage;


/**
 * @author Eugene.Kudelevsky
 */
public class XmlArrangementPanel extends ArrangementSettingsPanel {
    public XmlArrangementPanel(CodeStyleSettings settings) {
        super(settings, XMLLanguage.INSTANCE);
    }

    @Override
    protected int getRightMargin() {
        return 80;
    }

    @Override
    protected FileType getFileType() {
        return XmlFileType.INSTANCE;
    }

    @Override
    protected String getPreviewText() {
        return null;
    }
}
