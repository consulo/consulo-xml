package consulo.xml.ide.highlighter;

import com.intellij.xml.highlighter.EmbeddedTokenHighlighter;
import consulo.application.Application;
import consulo.colorScheme.TextAttributesKey;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.ast.IElementType;
import consulo.language.lexer.Lexer;
import consulo.util.collection.MultiMap;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lexer.XHtmlHighlightingLexer;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-04-18
 */
public class XHtmlFileHighlighter extends BaseXmlFileHighlighter {
  private static final ExtensionPointCacheKey<EmbeddedTokenHighlighter, MultiMap<IElementType, TextAttributesKey>> CACHE_KEY =
    ExtensionPointCacheKey.create("XHtmlFileHighlighter.tokens", embeddedTokenHighlighterExtensionWalker -> {
      MultiMap<IElementType, TextAttributesKey> map = MultiMap.createLinked();

      storeDefaults(map);

      embeddedTokenHighlighterExtensionWalker.walk(it -> map.putAllValues(it.getEmbeddedTokenAttributes(HTMLLanguage.INSTANCE)));

      return map;
    });

  public XHtmlFileHighlighter(Application application) {
    super(application);
  }

  @Override
  protected ExtensionPointCacheKey<EmbeddedTokenHighlighter, MultiMap<IElementType, TextAttributesKey>> getCacheKey() {
    return CACHE_KEY;
  }

  @Nonnull
  @Override
  public Lexer getHighlightingLexer() {
    return new XHtmlHighlightingLexer();
  }
}
