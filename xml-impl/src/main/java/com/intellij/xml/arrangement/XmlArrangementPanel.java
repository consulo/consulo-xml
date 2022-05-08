package com.intellij.xml.arrangement;

import javax.annotation.Nonnull;

import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.ide.impl.idea.application.options.codeStyle.arrangement.ArrangementSettingsPanel;

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
