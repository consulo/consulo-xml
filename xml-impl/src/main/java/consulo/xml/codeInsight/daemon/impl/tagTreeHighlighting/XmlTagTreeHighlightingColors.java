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
package consulo.xml.codeInsight.daemon.impl.tagTreeHighlighting;

import consulo.xml.application.options.editor.XmlEditorOptions;
import consulo.colorScheme.EditorColorKey;
import consulo.ui.color.ColorValue;
import consulo.ui.style.StandardColors;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlTagTreeHighlightingColors
{
	private static EditorColorKey[] ourColorKeys = null;

	private static final ColorValue[] DEFAULT_COLORS = {
			StandardColors.RED,
			StandardColors.YELLOW,
			StandardColors.GREEN,
			StandardColors.CYAN,
			StandardColors.BLUE,
			StandardColors.MAGENTA
	};

	private XmlTagTreeHighlightingColors()
	{
	}

	@Nonnull
	public static EditorColorKey[] getColorKeys()
	{
		final int levelCount = XmlEditorOptions.getInstance().getTagTreeHighlightingLevelCount();

		if(ourColorKeys == null || ourColorKeys.length != levelCount)
		{
			ourColorKeys = new EditorColorKey[levelCount];

			for(int i = 0; i < ourColorKeys.length; i++)
			{
				ourColorKeys[i] = EditorColorKey.createColorKey("HTML_TAG_TREE_LEVEL" + i, DEFAULT_COLORS[i % DEFAULT_COLORS.length]);
			}
		}

		return ourColorKeys;
	}
}
