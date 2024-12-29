package consulo.xml.psi.impl.cache.impl.idCache;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.lexer.Lexer;
import consulo.language.psi.stub.LexerBasedIdIndexer;
import consulo.language.psi.stub.OccurrenceConsumer;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.ide.highlighter.DTDFileType;
import consulo.xml.lexer.XmlLexer;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05-Jul-22
 */
@ExtensionImpl
public class DTDIdIndexer extends LexerBasedIdIndexer {
  public Lexer createLexer(final OccurrenceConsumer consumer) {
    return createIndexingLexer(consumer);
  }

  static XmlFilterLexer createIndexingLexer(OccurrenceConsumer consumer) {
    return new XmlFilterLexer(new XmlLexer(), consumer);
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return DTDFileType.INSTANCE;
  }
}
