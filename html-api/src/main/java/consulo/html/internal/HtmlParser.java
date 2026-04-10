package consulo.html.internal;

import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiParser;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public interface HtmlParser extends PsiParser {
    void parseDocument(PsiBuilder builder);
}
