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
package com.intellij.xml.index;

import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.IOUtil;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.io.Readers;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 7/4/12
 * Time: 6:29 PM
 * <p/>
 * map: tag name->file url
 */
public class SchemaTypeInheritanceIndex extends XmlIndex<Set<SchemaTypeInfo>>
{
	private static final ID<String, Set<SchemaTypeInfo>> NAME = ID.create("SchemaTypeInheritance");
	private static final Logger LOG = Logger.getInstance("#com.intellij.xml.index.SchemaTypeInheritanceIndex");

	private static List<Set<SchemaTypeInfo>> getDirectChildrenOfType(final Project project, final String ns, final String name)
	{
		GlobalSearchScope filter = createFilter(project);
		return FileBasedIndex.getInstance().getValues(NAME, NsPlusTag.INSTANCE.encode(Pair.create(ns, name)), filter);
	}

	public static BiFunction<String, String, List<Set<SchemaTypeInfo>>> getWorker(final Project project, final VirtualFile currentFile)
	{
		return new MyWorker(currentFile, project);
	}

	private static class MyWorker implements BiFunction<String, String, List<Set<SchemaTypeInfo>>>
	{
		private final Project myProject;
		private final VirtualFile myCurrentFile;
		private final GlobalSearchScope myFilter;
		private final boolean myShouldParseCurrent;
		private MultiMap<SchemaTypeInfo, SchemaTypeInfo> myMap;

		private MyWorker(VirtualFile currentFile, Project project)
		{
			myCurrentFile = currentFile;
			myProject = project;

			myFilter = createFilter(project);
			myShouldParseCurrent = (myCurrentFile != null && !myFilter.contains(myCurrentFile));
		}

		@Override
		public List<Set<SchemaTypeInfo>> apply(String ns, String name)
		{
			List<Set<SchemaTypeInfo>> type = getDirectChildrenOfType(myProject, ns, name);
			if(myShouldParseCurrent)
			{
				if(myMap == null)
				{
					try
					{
						myMap = XsdComplexTypeInfoBuilder.parse(Readers.readerFromCharSequence(VfsUtilCore.loadText(myCurrentFile)));
						type.add(new HashSet<SchemaTypeInfo>(myMap.get(new SchemaTypeInfo(name, true, ns))));
					}
					catch(IOException e)
					{
						LOG.info(e);
					}
				}
			}
			return type;
		}
	}

	@Override
	public boolean dependsOnFileContent()
	{
		return true;
	}

	@Override
	public int getVersion()
	{
		return 1;
	}

	@Nonnull
	@Override
	public ID<String, Set<SchemaTypeInfo>> getName()
	{
		return NAME;
	}

	@Nonnull
	@Override
	public DataIndexer<String, Set<SchemaTypeInfo>, FileContent> getIndexer()
	{
		return new DataIndexer<String, Set<SchemaTypeInfo>, FileContent>()
		{
			@Nonnull
			@Override
			public Map<String, Set<SchemaTypeInfo>> map(@Nonnull FileContent inputData)
			{
				final Map<String, Set<SchemaTypeInfo>> map = new HashMap<String, Set<SchemaTypeInfo>>();
				final MultiMap<SchemaTypeInfo, SchemaTypeInfo> multiMap = XsdComplexTypeInfoBuilder.parse(Readers.readerFromCharSequence(inputData.getContentAsText()));
				for(SchemaTypeInfo key : multiMap.keySet())
				{
					map.put(NsPlusTag.INSTANCE.encode(Pair.create(key.getNamespaceUri(), key.getTagName())), new HashSet<SchemaTypeInfo>(multiMap.get(key)));
				}
				return map;
			}
		};
	}

	@Nonnull
	@Override
	public DataExternalizer<Set<SchemaTypeInfo>> getValueExternalizer()
	{
		return new DataExternalizer<Set<SchemaTypeInfo>>()
		{
			@Override
			public void save(@Nonnull DataOutput out, Set<SchemaTypeInfo> value) throws IOException
			{
				DataInputOutputUtil.writeINT(out, value.size());
				for(SchemaTypeInfo key : value)
				{
					IOUtil.writeUTF(out, key.getNamespaceUri());
					IOUtil.writeUTF(out, key.getTagName());
					out.writeBoolean(key.isIsTypeName());
				}
			}

			@Override
			public Set<SchemaTypeInfo> read(@Nonnull DataInput in) throws IOException
			{
				final Set<SchemaTypeInfo> set = new HashSet<SchemaTypeInfo>();
				final int size = DataInputOutputUtil.readINT(in);
				for(int i = 0; i < size; i++)
				{
					final String nsUri = IOUtil.readUTF(in);
					final String tagName = IOUtil.readUTF(in);
					final boolean isType = in.readBoolean();
					set.add(new SchemaTypeInfo(tagName, isType, nsUri));
				}
				return set;
			}
		};
	}

	private static class NsPlusTag
	{
		private final static NsPlusTag INSTANCE = new NsPlusTag();
		private final static char ourSeparator = ':';

		public String encode(Pair<String, String> pair)
		{
			return pair.getFirst() + ourSeparator + pair.getSecond();
		}

		public Pair<String, String> decode(String s)
		{
			final int i = s.indexOf(ourSeparator);
			return i <= 0 ? Pair.create("", s) : Pair.create(s.substring(0, i), s.substring(i + 1));
		}
	}
}
