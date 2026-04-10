/**
 * @author VISTALL
 * @since 2026-04-10
 */
module com.intellij.xml.dom.api {
    requires consulo.language.api;
    requires consulo.language.editor.api;

    requires consulo.util.xml.fast.reader;

    requires com.intellij.xml.api;
    requires com.intellij.xml.editor.api;

    exports consulo.xml.dom;
    exports consulo.xml.dom.convert;
    exports consulo.xml.dom.reflect;
    exports consulo.xml.dom.editor;
    exports consulo.xml.dom.pattern;
    exports consulo.xml.dom.localize;
}