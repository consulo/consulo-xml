/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.html.index;

import com.intellij.xml.util.HtmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.file.LanguageFileType;
import consulo.language.lexer.Lexer;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.TempFileSystem;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.ide.highlighter.XHtmlFileType;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.xml.lexer.HtmlHighlightingLexer;
import consulo.xml.lexer.XHtmlHighlightingLexer;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class Html5CustomAttributesIndex extends ScalarIndexExtension<String> {
    public static final ID<String, Void> INDEX_ID = ID.create("html5.custom.attributes.index");

    private final DataIndexer<String, Void, FileContent> myIndexer = new DataIndexer<>() {
        @Override
        @Nonnull
        public Map<String, Void> map(FileContent inputData) {
            CharSequence input = inputData.getContentAsText();
            Language language = ((LanguageFileType)inputData.getFileType()).getLanguage();
            if (language == HTMLLanguage.INSTANCE || language == XHTMLLanguage.INSTANCE) {
                final Lexer lexer = (language == HTMLLanguage.INSTANCE ? new HtmlHighlightingLexer() : new XHtmlHighlightingLexer());
                lexer.start(input);
                Map<String, Void> result = new HashMap<>();
                IElementType tokenType = lexer.getTokenType();
                while (tokenType != null) {
                    if (tokenType == XmlTokenType.XML_NAME) {
                        String xmlName = input.subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString();
                        if (HtmlUtil.isCustomHtml5Attribute(xmlName)) {
                            result.put(xmlName, null);
                        }
                    }
                    else if (tokenType == XmlTokenType.XML_DOCTYPE_PUBLIC || tokenType == XmlTokenType.XML_DOCTYPE_SYSTEM) {
                        // this is not an HTML5 context
                        break;
                    }
                    lexer.advance();
                    tokenType = lexer.getTokenType();
                }
                return result;
            }
            return Collections.emptyMap();
        }
    };

    private final FileBasedIndex.InputFilter myInputFilter = (project, file) -> {
        if (file.getFileSystem() != LocalFileSystem.getInstance() && !(file.getFileSystem() instanceof TempFileSystem)) {
            return false;
        }

        final FileType fileType = file.getFileType();
        return fileType == HtmlFileType.INSTANCE || fileType == XHtmlFileType.INSTANCE;
    };

    @Nonnull
    @Override
    public ID<String, Void> getName() {
        return INDEX_ID;
    }

    @Nonnull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return myIndexer;
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return new EnumeratorStringDescriptor();
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return myInputFilter;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
