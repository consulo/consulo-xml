/**
 * @author VISTALL
 * @since 2026-04-10
 */
module com.intellij.xml.editor.api {
    requires transitive consulo.language.api;
    requires transitive consulo.language.editor.api;

    requires transitive com.intellij.xml.api;

    exports consulo.xml.editor;
}