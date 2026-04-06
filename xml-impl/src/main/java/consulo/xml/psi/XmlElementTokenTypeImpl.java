package consulo.xml.psi;

import consulo.language.ast.ASTNode;
import consulo.language.ast.CustomParsingType;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.util.CharTable;
import consulo.xml.lang.dtd.DTDLanguage;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.xml.language.XMLLanguage;
import consulo.xml.psi.impl.source.parsing.xml.DtdParsing;

/**
 * @author VISTALL
 * @since 2026-04-06
 */
public interface XmlElementTokenTypeImpl {
    IFileElementType HTML_FILE = new IFileElementType(HTMLLanguage.INSTANCE);

    IElementType XHTML_FILE = new IFileElementType(XHTMLLanguage.INSTANCE);

    IFileElementType DTD_FILE = new IFileElementType("DTD_FILE", DTDLanguage.INSTANCE);

    IElementType XML_MARKUP_DECL = new CustomParsingType("XML_MARKUP_DECL", XMLLanguage.INSTANCE) {
        public ASTNode parse(CharSequence text, CharTable table) {
            return new DtdParsing(text, XML_MARKUP_DECL, DtdParsing.TYPE_FOR_MARKUP_DECL, null).parse();
        }
    };
}
