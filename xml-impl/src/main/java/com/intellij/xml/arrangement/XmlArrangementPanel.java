package com.intellij.xml.arrangement;

import javax.annotation.Nonnull;

import com.intellij.application.options.codeStyle.arrangement.ArrangementSettingsPanel;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.codeStyle.CodeStyleSettings;

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
