/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.xml.index;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.HashMap;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.xml.util.XmlUtil;

/**
 * @author Dmitry Avdeev
 */
public class XmlTagNamesIndex extends XmlIndex<Void>
{

	public static Collection<VirtualFile> getFilesByTagName(String tagName, final Project project)
	{
		return FileBasedIndex.getInstance().getContainingFiles(NAME, tagName, createFilter(project));
	}

	public static Collection<String> getAllTagNames(Project project)
	{
		return FileBasedIndex.getInstance().getAllKeys(NAME, project);
	}

	static final ID<String, Void> NAME = ID.create("XmlTagNames");

	@Override
	@Nonnull
	public ID<String, Void> getName()
	{
		return NAME;
	}

	@Override
	@Nonnull
	public DataIndexer<String, Void, FileContent> getIndexer()
	{
		return new DataIndexer<String, Void, FileContent>()
		{
			@Override
			@Nonnull
			public Map<String, Void> map(@Nonnull final FileContent inputData)
			{
				CharSequence text = inputData.getContentAsText();
				if(StringUtil.indexOf(text, XmlUtil.XML_SCHEMA_URI) == -1)
				{
					return Collections.emptyMap();
				}
				Collection<String> tags = XsdTagNameBuilder.computeTagNames(CharArrayUtil.readerFromCharSequence(text));
				Map<String, Void> map = new HashMap<String, Void>(tags.size());
				for(String tag : tags)
				{
					map.put(tag, null);
				}
				return map;
			}
		};
	}

	@Nonnull
	@Override
	public DataExternalizer<Void> getValueExternalizer()
	{
		return ScalarIndexExtension.VOID_DATA_EXTERNALIZER;
	}
}
