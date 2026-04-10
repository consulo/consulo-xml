/**
 * @author VISTALL
 * @since 2026-04-10
 */
module com.intellij.xml.html.api {
    requires com.intellij.xml.api;

    requires consulo.util.xml.fast.reader;

    exports consulo.html.language;
    exports consulo.html.language.psi;
    exports consulo.html.language.psi.util;

    exports consulo.xhtml.language;

    exports consulo.html.internal to com.intellij.xml;
}