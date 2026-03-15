package com.intellij.xml.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.XHtmlFileType;


/**
 * @author VISTALL
 * @since 04-Jul-22
 */
@ExtensionImpl
public class XHtmlBraceMatcher extends XmlBaseBraceMatcher {
    @Override
    public FileType getFileType() {
        return XHtmlFileType.INSTANCE;
    }
}
