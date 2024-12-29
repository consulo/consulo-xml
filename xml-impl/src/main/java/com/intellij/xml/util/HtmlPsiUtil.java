package com.intellij.xml.util;

import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.language.psi.PsiFile;
import consulo.language.template.TemplateLanguageUtil;

import jakarta.annotation.Nullable;

public class HtmlPsiUtil {
    @Nullable
    public static XmlDocument getRealXmlDocument(@Nullable XmlDocument doc) {
        if (doc == null) {
            return null;
        }
        final PsiFile containingFile = doc.getContainingFile();

        final PsiFile templateFile = TemplateLanguageUtil.getTemplateFile(containingFile);
        return templateFile instanceof XmlFile templateXmlFile ? templateXmlFile.getDocument() : doc;
    }
}
