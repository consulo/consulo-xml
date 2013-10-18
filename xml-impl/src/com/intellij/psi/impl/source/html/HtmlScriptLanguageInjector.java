/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.psi.impl.source.html;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class HtmlScriptLanguageInjector implements MultiHostInjector {
  @Override
  public void injectLanguages(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement host) {
    if (!(host instanceof XmlText)) {
      return;
    }
    XmlTag scriptTag = ((XmlText)host).getParentTag();
    if (!"script".equalsIgnoreCase(scriptTag.getLocalName())) {
      return;
    }
    String mimeType = scriptTag.getAttributeValue("type");
    Collection<Language> languages = Language.findInstancesByMimeType(mimeType);
    Language language = languages.isEmpty() ? null : languages.iterator().next();
    if (language != null && InjectedLanguageUtil.isInjectableLanguage(language)) {
      registrar
        .startInjecting(language)
        .addPlace(null, null, (PsiLanguageInjectionHost)host, TextRange.create(0, host.getTextLength()))
        .doneInjecting();
    }
  }
}
