/*
 * Copyright 2013-2026 consulo.io
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
package consulo.xml.spellchecker.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.xhtml.language.XHTMLLanguage;
import consulo.xml.lang.dtd.DTDLanguage;
import consulo.xml.language.XMLLanguage;

@ExtensionImpl
public class XmlTextExtractorXml extends XmlTextExtractor {
    public XmlTextExtractorXml() {
        super(XMLLanguage.class, XHTMLLanguage.class, DTDLanguage.class);
    }

    @Override
    protected boolean shouldMaskEscapeSymbols() {
        return true;
    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
