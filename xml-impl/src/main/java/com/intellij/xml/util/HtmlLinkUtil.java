package com.intellij.xml.util;

import consulo.application.util.function.Processor;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.xml.*;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.List;

public class HtmlLinkUtil {
    @NonNls
    public static final String LINK = "link";

    public static void processLinks(@Nonnull final XmlFile xhtmlFile, @Nonnull Processor<XmlTag> tagProcessor) {
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

    public static void findLinkStylesheets(@Nonnull final XmlTag tag, @Nonnull Processor<XmlTag> tagProcessor) {
        processInjectedContent(tag, tagProcessor);

        for (XmlTag subTag : tag.getSubTags()) {
            findLinkStylesheets(subTag, tagProcessor);
        }

        if (LINK.equalsIgnoreCase(tag.getName())) {
            tagProcessor.process(tag);
        }
    }

    public static void processInjectedContent(final XmlTag element, @Nonnull final Processor<XmlTag> tagProcessor) {
        final PsiLanguageInjectionHost.InjectedPsiVisitor injectedPsiVisitor = (injectedPsi, places) -> {
            if (injectedPsi instanceof XmlFile xmlFile) {
                final XmlDocument injectedDocument = xmlFile.getDocument();
                if (injectedDocument != null) {
                    final XmlTag rootTag = injectedDocument.getRootTag();
                    if (rootTag != null) {
                        for (PsiElement element1 = rootTag; element1 != null; element1 = element1.getNextSibling()) {
                            if (element1 instanceof XmlTag) {
                                final XmlTag tag = (XmlTag)element1;
                                String tagName = tag.getLocalName();
                                if (element1 instanceof HtmlTag || tag.getNamespacePrefix().length() > 0) {
                                    tagName = tagName.toLowerCase();
                                }
                                if (LINK.equalsIgnoreCase(tagName)) {
                                    tagProcessor.process((XmlTag)element1);
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
