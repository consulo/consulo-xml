package com.intellij.xml.util;

import consulo.application.util.function.Processor;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.language.psi.*;
import consulo.html.language.psi.HtmlTag;

public class HtmlLinkUtil {
    public static final String LINK = "link";

    public static void processLinks(final XmlFile xhtmlFile, Processor<XmlTag> tagProcessor) {
        final XmlDocument doc = HtmlUtil.getRealXmlDocument(xhtmlFile.getDocument());
        if (doc == null) {
            return;
        }

        final XmlTag rootTag = doc.getRootTag();
        if (rootTag == null) {
            return;
        }

        if (LINK.equalsIgnoreCase(rootTag.getName())) {
            tagProcessor.process(rootTag);
        }
        else {
            findLinkStylesheets(rootTag, tagProcessor);
        }
    }

    public static void findLinkStylesheets(final XmlTag tag, Processor<XmlTag> tagProcessor) {
        processInjectedContent(tag, tagProcessor);

        for (XmlTag subTag : tag.getSubTags()) {
            findLinkStylesheets(subTag, tagProcessor);
        }

        if (LINK.equalsIgnoreCase(tag.getName())) {
            tagProcessor.process(tag);
        }
    }

    public static void processInjectedContent(final XmlTag element, final Processor<XmlTag> tagProcessor) {
        final PsiLanguageInjectionHost.InjectedPsiVisitor injectedPsiVisitor = (injectedPsi, places) -> {
            if (injectedPsi instanceof XmlFile xmlFile) {
                final XmlDocument injectedDocument = xmlFile.getDocument();
                if (injectedDocument != null) {
                    final XmlTag rootTag = injectedDocument.getRootTag();
                    if (rootTag != null) {
                        for (PsiElement htmlElement = rootTag; htmlElement != null; htmlElement = htmlElement.getNextSibling()) {
                            if (htmlElement instanceof XmlTag tag) {
                                String tagName = tag.getLocalName();
                                if (htmlElement instanceof HtmlTag || tag.getNamespacePrefix().length() > 0) {
                                    tagName = tagName.toLowerCase();
                                }
                                if (LINK.equalsIgnoreCase(tagName)) {
                                    tagProcessor.process(tag);
                                }
                            }
                        }
                    }
                }
            }
        };

        InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(element.getProject());

        final XmlText[] texts = PsiTreeUtil.getChildrenOfType(element, XmlText.class);
        if (texts != null && texts.length > 0) {
            for (final XmlText text : texts) {
                for (PsiElement _element : text.getChildren()) {
                    if (_element instanceof PsiLanguageInjectionHost) {
                        injectedLanguageManager.enumerate(_element, injectedPsiVisitor);
                    }
                }
            }
        }

        final XmlComment[] comments = PsiTreeUtil.getChildrenOfType(element, XmlComment.class);
        if (comments != null && comments.length > 0) {
            for (final XmlComment comment : comments) {
                if (comment instanceof PsiLanguageInjectionHost) {
                    injectedLanguageManager.enumerate(comment, injectedPsiVisitor);
                }
            }
        }
    }
}
