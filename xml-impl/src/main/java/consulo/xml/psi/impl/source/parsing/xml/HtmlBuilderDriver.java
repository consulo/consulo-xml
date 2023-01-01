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

/*
 * @author max
 */
package consulo.xml.psi.impl.source.parsing.xml;

import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.html.HtmlParsing;

public class HtmlBuilderDriver extends XmlBuilderDriver {
  public HtmlBuilderDriver(final CharSequence text) {
    super(text);
  }

  @Override
  protected PsiBuilder createBuilderAndParse() {
    final ParserDefinition htmlParserDef = ParserDefinition.forLanguage(HTMLLanguage.INSTANCE);
    assert htmlParserDef != null;

    LanguageVersion defaultVersion = LanguageVersionUtil.findDefaultVersion(HTMLLanguage.INSTANCE);
    PsiBuilder b = PsiBuilderFactory.getInstance()
      .createBuilder(htmlParserDef, htmlParserDef.createLexer(defaultVersion), defaultVersion, getText());
    new HtmlParsing(b).parseDocument();
    return b;
  }
}
