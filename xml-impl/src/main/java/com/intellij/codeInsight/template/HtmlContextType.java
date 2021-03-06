/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.template;

import javax.annotation.Nonnull;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xhtml.XHTMLLanguage;
import com.intellij.psi.PsiFile;

/**
 * @author yole
 */
public class HtmlContextType extends FileTypeBasedContextType {
  public HtmlContextType() {
    super("HTML", CodeInsightBundle.message("dialog.edit.template.checkbox.html"), HtmlFileType.INSTANCE);
  }

  @Override
  public boolean isInContext(@Nonnull PsiFile file, int offset) {
    return isMyLanguage(file.getLanguage()) && !XmlContextType.isEmbeddedContent(file, offset);
  }

  static boolean isMyLanguage(Language language) {
    return language.isKindOf(HTMLLanguage.INSTANCE) || language.isKindOf(XHTMLLanguage.INSTANCE);
  }
}