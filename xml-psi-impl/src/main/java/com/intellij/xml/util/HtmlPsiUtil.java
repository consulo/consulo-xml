package com.intellij.xml.util;

import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import consulo.language.psi.PsiFile;

import javax.annotation.Nullable;

public class HtmlPsiUtil {
  @Nullable
  public static XmlDocument getRealXmlDocument(@Nullable XmlDocument doc) {
    if (doc == null) return null;
    final PsiFile containingFile = doc.getContainingFile();

    final PsiFile templateFile = TemplateLanguageUtil.getTemplateFile(containingFile);
    if (templateFile instanceof XmlFile) {
      return ((XmlFile)templateFile).getDocument();
    }
    return doc;
  }
}
