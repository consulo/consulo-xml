/**
 * @author VISTALL
 * @since 2026-04-06
 */
module com.intellij.xml.api {
    requires consulo.language.api;

    requires static consulo.util.xml.fast.reader;

    exports consulo.xml.descriptor;
    exports consulo.xml.descriptor.xsd;
    exports consulo.xml.language;
    exports consulo.xml.language.psi;
    exports consulo.xml.language.psi.pattern;
    exports consulo.xml.standardResource;
    exports consulo.xml.language.psi.util;

    exports consulo.xml.internal to com.intellij.xml;
}