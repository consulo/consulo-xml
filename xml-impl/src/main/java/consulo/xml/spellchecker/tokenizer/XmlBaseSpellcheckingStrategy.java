/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.xml.spellchecker.tokenizer;

import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.spellcheker.SpellcheckingStrategy;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import consulo.language.spellcheker.tokenizer.splitter.TextTokenSplitter;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlText;

import javax.annotation.Nonnull;

public abstract class XmlBaseSpellcheckingStrategy extends SpellcheckingStrategy {
  protected final Tokenizer<XmlAttributeValue> myXmlAttributeTokenizer = new XmlAttributeValueTokenizer();
  protected final Tokenizer<XmlText> myXmlTextTokenizer = new XmlTextTokenizer();

  @Nonnull
  public Tokenizer getTokenizer(PsiElement element) {
    if (element instanceof XmlAttributeValue) {
      return myXmlAttributeTokenizer;
    }
    if (element instanceof XmlText) {
      return myXmlTextTokenizer;
    }

    return SpellcheckingStrategy.EMPTY_TOKENIZER;
  }

  public static class XmlAttributeValueTokenizer extends Tokenizer<XmlAttributeValue> {
    public void tokenize(@Nonnull final XmlAttributeValue element, final TokenConsumer consumer) {
      if (element instanceof PsiLanguageInjectionHost && InjectedLanguageManager.getInstance(element.getProject()).getInjectedPsiFiles(element) != null) {
        return;
      }

      final String valueTextTrimmed = element.getValue().trim();
      // do not inspect colors like #00aaFF
      if (valueTextTrimmed.startsWith("#") && valueTextTrimmed.length() <= 7 && isHexString(valueTextTrimmed.substring(1))) {
        return;
      }

      consumer.consumeToken(element, TextTokenSplitter.getInstance());
    }

    private static boolean isHexString(final String s) {
      for (int i = 0; i < s.length(); i++) {
        if (!StringUtil.isHexDigit(s.charAt(i))) {
          return false;
        }
      }
      return true;
    }
  }
}
