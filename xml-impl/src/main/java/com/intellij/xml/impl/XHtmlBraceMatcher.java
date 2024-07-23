package com.intellij.xml.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XHtmlFileType;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04-Jul-22
 */
@ExtensionImpl
public class XHtmlBraceMatcher extends XmlBaseBraceMatcher {
    @Nonnull
    @Override
    public FileType getFileType() {
        return XHtmlFileType.INSTANCE;
    }
}
