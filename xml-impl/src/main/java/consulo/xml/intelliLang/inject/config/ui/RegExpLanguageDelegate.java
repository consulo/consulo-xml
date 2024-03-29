/*
 * Copyright 2013 Consulo.org
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
package consulo.xml.intelliLang.inject.config.ui;

import consulo.component.util.pointer.NamedPointer;
import consulo.language.Language;
import consulo.language.LanguagePointerUtil;

/**
 * @author VISTALL
 * @since 7:54/31.05.13
 * <p/>
 * IDE can be without org.intellij.lang.regexp.RegExpLanguage class, and it ill produce ClassNotFoundException
 */
public class RegExpLanguageDelegate
{
	public static final NamedPointer<Language> RegExp = LanguagePointerUtil.createPointer("RegExp");
}
