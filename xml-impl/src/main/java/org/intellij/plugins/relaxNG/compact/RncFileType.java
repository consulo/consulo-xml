/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.compact;

import consulo.application.AllIcons;
import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
 * User: sweinreuter
 * Date: 01.08.2007
 */
public class RncFileType extends LanguageFileType
{
	public static final String RNC_EXT = "rnc";

	public static final FileType INSTANCE = new RncFileType();

	private RncFileType()
	{
		super(RngCompactLanguage.INSTANCE);
	}

	@Override
	@Nonnull
	public String getId()
	{
		return "RNG Compact";
	}

	@Override
	@Nonnull
	public LocalizeValue getDescription()
	{
		return LocalizeValue.localizeTODO("RELAX NG Compact Syntax");
	}

	@Override
	@Nonnull
	public String getDefaultExtension()
	{
		return "rnc";
	}

	@Override
	@Nullable
	public Image getIcon()
	{
		return AllIcons.FileTypes.Text;
	}

	public static FileType getInstance()
	{
		return INSTANCE;
	}

}
