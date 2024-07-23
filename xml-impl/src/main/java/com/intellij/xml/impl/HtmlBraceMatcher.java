package com.intellij.xml.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.HtmlFileType;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04-Jul-22
 */
@ExtensionImpl
public class HtmlBraceMatcher extends XmlBaseBraceMatcher {
    @Nonnull
    @Override
    public FileType getFileType() {
        return HtmlFileType.INSTANCE;
    }
}
