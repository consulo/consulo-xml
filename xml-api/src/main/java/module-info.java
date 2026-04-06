/**
 * @author VISTALL
 * @since 2026-04-06
 */
module com.intellij.xml.api {
    requires consulo.language.api;

    exports consulo.xml.descriptor;
    exports consulo.xml.language;
    exports consulo.xml.language.psi;
    exports consulo.xml.standardResource;
    exports consulo.xml.language.psi.util;
}