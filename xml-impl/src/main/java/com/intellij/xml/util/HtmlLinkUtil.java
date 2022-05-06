package com.intellij.xml.util;

import com.intellij.psi.html.HtmlTag;
import consulo.language.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import consulo.application.util.function.Processor;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.List;

public class HtmlLinkUtil {
  @NonNls public static final String LINK = "link";

  public static void processLinks(@Nonnull final XmlFile xhtmlFile,
                                  @Nonnull Processor<XmlTag> tagProcessor) {
    final XmlDocument doc = HtmlUtil.getRealXmlDocument(xhtmlFile.getDocument());
    if (doc == null) return;

    final XmlTag rootTag = doc.getRootTag();
    if (rootTag == null) return;

    if (LINK.equalsIgnoreCase(rootTag.getName())) {
      tagProcessor.process(rootTag);
    }
    else {
      findLinkStylesheets(rootTag, tagProcessor);
    }
  }

  public static void findLinkStylesheets(@Nonnull final XmlTag tag,
                                         @Nonnull Processor<XmlTag> tagProcessor) {
    processInjectedContent(tag, tagProcessor);

    for (XmlTag subTag : tag.getSubTags()) {
      findLinkStylesheets(subTag, tagProcessor);
    }

    if (LINK.equalsIgnoreCase(tag.getName())) {
      tagProcessor.process(tag);
    }
  }

  public static void processInjectedContent(final XmlTag element,
                                            @Nonnull final Processor<XmlTag> tagProcessor) {
    final PsiLanguageInjectionHost.InjectedPsiVisitor injectedPsiVisitor = new PsiLanguageInjectionHost.InjectedPsiVisitor() {
      @Override
      public void visit(@Nonnull PsiFile injectedPsi, @Nonnull List<PsiLanguageInjectionHost.Shred> places) {
        if (injectedPsi instanceof XmlFile) {
          final XmlDocument injectedDocument = ((XmlFile)injectedPsi).getDocument();
          if (injectedDocument != null) {
            final XmlTag rootTag = injectedDocument.getRootTag();
            if (rootTag != null) {
              for (PsiElement element = rootTag; element != null; element = element.getNextSibling()) {
                if (element instanceof XmlTag) {
                  final XmlTag tag = (XmlTag)element;
                  String tagName = tag.getLocalName();
                  if (element instanceof HtmlTag || tag.getNamespacePrefix().length() > 0) tagName = tagName.toLowerCase();
                  if (LINK.equalsIgnoreCase(tagName)) {
                    tagProcessor.process((XmlTag)element);
                  }
                }
              }
            }
          }
        }
      }
    };

    final XmlText[] texts = PsiTreeUtil.getChildrenOfType(element, XmlText.class);
    if (texts != null && texts.length > 0) {
      for (final XmlText text : texts) {
        for (PsiElement _element : text.getChildren()) {
          if (_element instanceof PsiLanguageInjectionHost) {
            InjectedLanguageUtil.enumerate(_element, injectedPsiVisitor);
          }
        }
      }
    }

    final XmlComment[] comments = PsiTreeUtil.getChildrenOfType(element, XmlComment.class);
    if (comments != null && comments.length > 0) {
      for (final XmlComment comment : comments) {
        if (comment instanceof PsiLanguageInjectionHost) {
          InjectedLanguageUtil.enumerate(comment, injectedPsiVisitor);
        }
      }
    }
  }
}
