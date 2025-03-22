/**
 * @author VISTALL
 * @since 2025-03-22
 */
module com.intellij.xml.jingtrang {
    requires relaxngDatatype;
    requires java.xml;
    requires isorelax;
    requires xercesImpl;
    requires xml.resolver;

    exports com.thaiopensource.datatype;
    exports com.thaiopensource.datatype.xsd;
    exports com.thaiopensource.datatype.xsd.regex;
    exports com.thaiopensource.datatype.xsd.regex.java;
    exports com.thaiopensource.datatype.xsd.regex.java.gen;
    exports com.thaiopensource.datatype.xsd.regex.xerces2;
    exports com.thaiopensource.relaxng;
    exports com.thaiopensource.relaxng.edit;
    exports com.thaiopensource.relaxng.input;
    exports com.thaiopensource.relaxng.input.dtd;
    exports com.thaiopensource.relaxng.input.parse;
    exports com.thaiopensource.relaxng.input.parse.compact;
    exports com.thaiopensource.relaxng.input.parse.sax;
    exports com.thaiopensource.relaxng.input.xml;
    exports com.thaiopensource.relaxng.jarv;
    exports com.thaiopensource.relaxng.jaxp;
    exports com.thaiopensource.relaxng.match;
    exports com.thaiopensource.relaxng.output;
    exports com.thaiopensource.relaxng.output.common;
    exports com.thaiopensource.relaxng.output.dtd;
    exports com.thaiopensource.relaxng.output.rnc;
    exports com.thaiopensource.relaxng.output.rng;
    exports com.thaiopensource.relaxng.output.xsd;
    exports com.thaiopensource.relaxng.output.xsd.basic;
    exports com.thaiopensource.relaxng.parse;
    exports com.thaiopensource.relaxng.parse.compact;
    exports com.thaiopensource.relaxng.parse.sax;
    exports com.thaiopensource.relaxng.pattern;
    exports com.thaiopensource.relaxng.sax;
    exports com.thaiopensource.relaxng.translate;
    exports com.thaiopensource.relaxng.translate.test;
    exports com.thaiopensource.relaxng.translate.util;
    exports com.thaiopensource.relaxng.util;
    exports com.thaiopensource.resolver;
    exports com.thaiopensource.resolver.catalog;
    exports com.thaiopensource.resolver.load;
    exports com.thaiopensource.resolver.xml;
    exports com.thaiopensource.resolver.xml.ls;
    exports com.thaiopensource.resolver.xml.sax;
    exports com.thaiopensource.resolver.xml.transform;
    exports com.thaiopensource.util;
    exports com.thaiopensource.validate;
    exports com.thaiopensource.validate.auto;
    exports com.thaiopensource.validate.jarv;
    exports com.thaiopensource.validate.mns;
    exports com.thaiopensource.validate.nrl;
    exports com.thaiopensource.validate.nvdl;
    exports com.thaiopensource.validate.picl;
    exports com.thaiopensource.validate.prop.rng;
    exports com.thaiopensource.validate.prop.schematron;
    exports com.thaiopensource.validate.prop.wrap;
    exports com.thaiopensource.validate.rng;
    exports com.thaiopensource.validate.rng.impl;
    exports com.thaiopensource.validate.schematron;
    exports com.thaiopensource.validation;
    exports com.thaiopensource.xml.dtd.om;
    exports com.thaiopensource.xml.dtd.parse;
    exports com.thaiopensource.xml.em;
    exports com.thaiopensource.xml.infer;
    exports com.thaiopensource.xml.out;
    exports com.thaiopensource.xml.sax;
    exports com.thaiopensource.xml.tok;
    exports com.thaiopensource.xml.util;
}