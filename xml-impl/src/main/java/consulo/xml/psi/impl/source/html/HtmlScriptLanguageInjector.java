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
package consulo.xml.psi.impl.source.html;

import java.util.Collection;

import javax.annotation.Nonnull;

import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.util.LanguageUtil;
import consulo.language.inject.MultiHostInjector;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;

public class HtmlScriptLanguageInjector implements MultiHostInjector {
  @Override
  public void injectLanguages(@Nonnull MultiHostRegistrar registrar, @Nonnull PsiElement host) {
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
    if (language != null && LanguageUtil.isInjectableLanguage(language)) {
      registrar
        .startInjecting(language)
        .addPlace(null, null, (PsiLanguageInjectionHost)host, TextRange.create(0, host.getTextLength()))
        .doneInjecting();
    }
  }
}
