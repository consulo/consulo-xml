/**
 * @author VISTALL
 * @since 2025-03-22
 */
module com.intellij.xml.rngom {
    exports org.kohsuke.rngom.ast.builder;
    exports org.kohsuke.rngom.ast.om;
    exports org.kohsuke.rngom.ast.util;
    exports org.kohsuke.rngom.binary;
    exports org.kohsuke.rngom.binary.visitor;
    exports org.kohsuke.rngom.digested;
    exports org.kohsuke.rngom.dt;
    exports org.kohsuke.rngom.dt.builtin;
    exports org.kohsuke.rngom.nc;
    exports org.kohsuke.rngom.parse;
    exports org.kohsuke.rngom.parse.compact;
    exports org.kohsuke.rngom.parse.host;
    exports org.kohsuke.rngom.parse.xml;
    exports org.kohsuke.rngom.util;
    exports org.kohsuke.rngom.xml.sax;
    exports org.kohsuke.rngom.xml.util;

    requires relaxngDatatype;
    requires java.xml;
}