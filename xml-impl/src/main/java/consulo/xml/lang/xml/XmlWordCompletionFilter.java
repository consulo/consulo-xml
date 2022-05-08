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
package consulo.xml.lang.xml;

import consulo.language.ast.TokenSet;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTokenType;
import consulo.ide.impl.idea.lang.DefaultWordCompletionFilter;
import consulo.language.ast.IElementType;
import consulo.language.version.LanguageVersion;

public class XmlWordCompletionFilter extends DefaultWordCompletionFilter {
  private final static TokenSet ENABLED_TOKENS = TokenSet.create(XmlElementType.XML_CDATA,
                                                                 XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN,
                                                                 XmlTokenType.XML_DATA_CHARACTERS);
  public boolean isWordCompletionEnabledIn(final IElementType element, LanguageVersion languageVersion) {
    return super.isWordCompletionEnabledIn(element, languageVersion) || ENABLED_TOKENS.contains(element);
  }
}