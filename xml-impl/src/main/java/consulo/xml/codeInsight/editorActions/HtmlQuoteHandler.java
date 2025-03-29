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
package consulo.xml.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.editor.action.FileQuoteHandler;
import consulo.language.editor.action.QuoteHandler;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.HtmlFileType;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl
public class HtmlQuoteHandler implements FileQuoteHandler {
    private QuoteHandler myBaseQuoteHandler;

    public HtmlQuoteHandler() {
        this(new XmlBasedQuoteHandler());
    }

    public HtmlQuoteHandler(QuoteHandler _baseHandler) {
        myBaseQuoteHandler = _baseHandler;
    }

    @Override
    public boolean isClosingQuote(HighlighterIterator iterator, int offset) {
        return myBaseQuoteHandler.isClosingQuote(iterator, offset);
    }

    @Override
    public boolean isOpeningQuote(HighlighterIterator iterator, int offset) {
        return myBaseQuoteHandler.isOpeningQuote(iterator, offset);
    }

    @Override
    public boolean hasNonClosedLiteral(Editor editor, HighlighterIterator iterator, int offset) {
        return myBaseQuoteHandler.hasNonClosedLiteral(editor, iterator, offset);
    }

    @Override
    public boolean isInsideLiteral(HighlighterIterator iterator) {
        return myBaseQuoteHandler.isInsideLiteral(iterator);
    }

    @Nonnull
    @Override
    public FileType getFileType() {
        return HtmlFileType.INSTANCE;
    }
}
