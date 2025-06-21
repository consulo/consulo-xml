/**
 * @author VISTALL
 * @since 07-May-22
 */
open module com.intellij.xml
{
    requires transitive consulo.ide.api;

    requires consulo.util.xml.fast.reader;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;

    // TODO remove in future
    requires consulo.ide.impl;
    requires consulo.language.impl;

    requires xml.resolver;

    requires com.intellij.xml.rngom;
    requires com.intellij.xml.jingtrang;

    requires relaxngDatatype;

    requires xmlbeans;
    requires jaxen;
    requires xercesImpl;

    exports com.intellij.html;
    exports com.intellij.html.impl;
    exports com.intellij.html.impl.providers;
    exports com.intellij.html.impl.util;
    exports com.intellij.html.index;
    exports com.intellij.xml;
    exports com.intellij.xml.actions;
    exports com.intellij.xml.actions.validate;
    exports com.intellij.xml.actions.xmlbeans;
    exports com.intellij.xml.arrangement;
    exports com.intellij.xml.config;
    exports com.intellij.xml.impl;
    exports com.intellij.xml.impl.dom;
    exports com.intellij.xml.impl.dtd;
    exports com.intellij.xml.impl.schema;
    exports com.intellij.xml.index;
    exports com.intellij.xml.refactoring;
    exports com.intellij.xml.util;
    exports com.intellij.xml.util.documentation;
    exports consulo.relaxng;
    exports consulo.xml;
    exports consulo.xml.application.options;
    exports consulo.xml.application.options.editor;
    exports consulo.xml.codeInsight.actions;
    exports consulo.xml.codeInsight.completion;
    exports consulo.xml.codeInsight.daemon;
    exports consulo.xml.codeInsight.daemon.impl.analysis;
    exports consulo.xml.codeInsight.daemon.impl.analysis.encoding;
    exports consulo.xml.codeInsight.daemon.impl.quickfix;
    exports consulo.xml.codeInsight.daemon.impl.tagTreeHighlighting;
    exports consulo.xml.codeInsight.editorActions;
    exports consulo.xml.codeInsight.editorActions.moveLeftRight;
    exports consulo.xml.codeInsight.editorActions.moveUpDown;
    exports consulo.xml.codeInsight.folding.impl;
    exports consulo.xml.codeInsight.highlighting;
    exports consulo.xml.codeInsight.hint;
    exports consulo.xml.codeInsight.hint.api.impls;
    exports consulo.xml.codeInsight.intentions;
    exports consulo.xml.codeInsight.navigation;
    exports consulo.xml.codeInsight.template;
    exports consulo.xml.codeInspection;
    exports consulo.xml.codeInspection.htmlInspections;
    exports consulo.xml.copyright.psi;
    exports consulo.xml.dom.util.proxy;
    exports consulo.xml.editor;
    exports consulo.xml.editor.bidi;
    exports consulo.xml.featureStatistics;
    exports consulo.xml.ide.actions;
    exports consulo.xml.ide.highlighter;
    exports consulo.xml.ide.structureView.impl.xml;
    exports consulo.xml.ide.structureView.xml;
    exports consulo.xml.impl.dom;
    exports consulo.xml.impl.localize;
    exports consulo.xml.intelliLang.inject.config;
    exports consulo.xml.intelliLang.inject.config.ui;
    exports consulo.xml.intelliLang.inject.config.ui.configurables;
    exports consulo.xml.intelliLang.inject.xml;
    exports consulo.xml.javaee;
    exports consulo.xml.javaee.web;
    exports consulo.xml.lang;
    exports consulo.xml.lang.base;
    exports consulo.xml.lang.documentation;
    exports consulo.xml.lang.dtd;
    exports consulo.xml.lang.html;
    exports consulo.xml.lang.html.structureView;
    exports consulo.xml.lang.xhtml;
    exports consulo.xml.lang.xml;
    exports consulo.xml.lexer;
    exports consulo.xml.navbar;
    exports consulo.xml.navigation;
    exports consulo.xml.options.colors.pages;
    exports consulo.xml.patterns;
    exports consulo.xml.psi;
    exports consulo.xml.psi.filters;
    exports consulo.xml.psi.filters.getters;
    exports consulo.xml.psi.filters.position;
    exports consulo.xml.psi.formatter;
    exports consulo.xml.psi.formatter.xml;
    exports consulo.xml.psi.html;
    exports consulo.xml.psi.impl.cache.impl.idCache;
    exports consulo.xml.psi.impl.smartPointers;
    exports consulo.xml.psi.impl.source.html;
    exports consulo.xml.psi.impl.source.html.dtd;
    exports consulo.xml.psi.impl.source.parsing.xml;
    exports consulo.xml.psi.impl.source.resolve.reference.impl.manipulators;
    exports consulo.xml.psi.impl.source.resolve.reference.impl.providers;
    exports consulo.xml.psi.impl.source.tree;
    exports consulo.xml.psi.impl.source.tree.injected;
    exports consulo.xml.psi.impl.source.xml;
    exports consulo.xml.psi.impl.source.xml.behavior;
    exports consulo.xml.psi.tree.xml;
    exports consulo.xml.psi.xml;
    exports consulo.xml.refactoring;
    exports consulo.xml.refactoring.rename;
    exports consulo.xml.refactoring.util;
    exports consulo.xml.spellchecker.tokenizer;
    exports consulo.xml.spellchecker.xml;
    exports consulo.xml.usageView;
    exports consulo.xml.util;
    exports consulo.xml.util.xml;
    exports consulo.xml.util.xml.actions.generate;
    exports consulo.xml.util.xml.converters;
    exports consulo.xml.util.xml.converters.values;
    exports consulo.xml.util.xml.events;
    exports consulo.xml.util.xml.highlighting;
    exports consulo.xml.util.xml.impl;
    exports consulo.xml.util.xml.model;
    exports consulo.xml.util.xml.model.gotosymbol;
    exports consulo.xml.util.xml.model.impl;
    exports consulo.xml.util.xml.reflect;
    exports consulo.xml.util.xml.structure;
    exports consulo.xml.util.xml.stubs;
    exports consulo.xml.util.xml.stubs.builder;
    exports consulo.xml.util.xml.stubs.index;
    exports consulo.xml.util.xml.tree;
    exports consulo.xml.util.xml.tree.actions;
    exports consulo.xml.util.xml.ui;
    exports consulo.xml.util.xml.ui.actions;
    exports consulo.xml.util.xml.ui.actions.generate;
    exports consulo.xml.localize;
    exports consulo.xml.vcsUtil;
    exports consulo.xml.dom;
    exports com.intellij.xml.highlighter;
    exports org.intellij.html;
    exports org.intellij.plugins.relaxNG;
    exports org.intellij.plugins.relaxNG.compact;
    exports org.intellij.plugins.relaxNG.compact.folding;
    exports org.intellij.plugins.relaxNG.compact.formatting;
    exports org.intellij.plugins.relaxNG.compact.lexer;
    exports org.intellij.plugins.relaxNG.compact.parser;
    exports org.intellij.plugins.relaxNG.compact.psi;
    exports org.intellij.plugins.relaxNG.compact.psi.impl;
    exports org.intellij.plugins.relaxNG.compact.psi.util;
    exports org.intellij.plugins.relaxNG.convert;
    exports org.intellij.plugins.relaxNG.inspections;
    exports org.intellij.plugins.relaxNG.model;
    exports org.intellij.plugins.relaxNG.model.annotation;
    exports org.intellij.plugins.relaxNG.model.descriptors;
    exports org.intellij.plugins.relaxNG.model.resolve;
    exports org.intellij.plugins.relaxNG.references;
    exports org.intellij.plugins.relaxNG.validation;
    exports org.intellij.plugins.relaxNG.xml;
    exports org.intellij.plugins.relaxNG.xml.dom;
    exports org.intellij.plugins.relaxNG.xml.dom.impl;
    exports org.intellij.plugins.relaxNG.xml.dom.names;

    //opens consulo.xml.psi.impl.source.xml to consulo.util.concurrent;
}