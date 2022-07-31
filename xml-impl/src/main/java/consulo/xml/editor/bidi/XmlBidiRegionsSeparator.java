/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.xml.editor.bidi;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.TokenSet;
import consulo.language.editor.bidi.TokenSetBidiRegionsSeparator;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

@ExtensionImpl
public class XmlBidiRegionsSeparator extends TokenSetBidiRegionsSeparator
{
	public XmlBidiRegionsSeparator()
	{
		super(TokenSet.create(XmlTokenType.XML_DATA_CHARACTERS, XmlTokenType.XML_REAL_WHITE_SPACE));
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
