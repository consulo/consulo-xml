package com.intellij.xml.arrangement;

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.ui.setting.arrangement.ArrangementSettingsPanel;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.lang.xml.XMLLanguage;

import javax.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlArrangementPanel extends ArrangementSettingsPanel {
    public XmlArrangementPanel(@Nonnull CodeStyleSettings settings) {
        super(settings, XMLLanguage.INSTANCE);
    }

    @Override
    protected int getRightMargin() {
        return 80;
    }

    @Nonnull
    @Override
    protected FileType getFileType() {
        return XmlFileType.INSTANCE;
    }

    @Override
    protected String getPreviewText() {
        return null;
    }
}
