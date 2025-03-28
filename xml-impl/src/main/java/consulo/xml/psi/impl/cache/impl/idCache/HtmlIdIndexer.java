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
package consulo.xml.psi.impl.cache.impl.idCache;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.lexer.Lexer;
import consulo.language.psi.stub.LexerBasedIdIndexer;
import consulo.language.psi.stub.OccurrenceConsumer;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.lexer.HtmlHighlightingLexer;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class HtmlIdIndexer extends LexerBasedIdIndexer {
  public Lexer createLexer(final OccurrenceConsumer consumer) {
    return createIndexingLexer(consumer);
  }

  static XHtmlFilterLexer createIndexingLexer(OccurrenceConsumer consumer) {
    return new XHtmlFilterLexer(new HtmlHighlightingLexer(), consumer);
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return HtmlFileType.INSTANCE;
  }
}
