package com.intellij.xml.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.DTDFileType;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04-Jul-22
 */
@ExtensionImpl
public class DTDBraceMatcher extends XmlBaseBraceMatcher {
    @Nonnull
    @Override
    public FileType getFileType() {
        return DTDFileType.INSTANCE;
    }
}
