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

package org.intellij.plugins.relaxNG.validation;

import com.thaiopensource.datatype.xsd.DatatypeLibraryFactoryImpl;
import com.thaiopensource.relaxng.pattern.AnnotationsImpl;
import com.thaiopensource.relaxng.pattern.CommentListImpl;
import com.thaiopensource.relaxng.pattern.NameClass;
import com.thaiopensource.relaxng.pattern.Pattern;
import com.thaiopensource.resolver.Input;
import com.thaiopensource.resolver.xml.sax.SAX;
import com.thaiopensource.resolver.xml.sax.SAXResolver;
import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.util.VoidValue;
import com.thaiopensource.validate.Schema;
import com.thaiopensource.validate.rng.impl.SchemaReaderImpl;
import consulo.application.progress.ProgressManager;
import consulo.application.util.*;
import consulo.language.psi.PsiFile;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.javaee.UriUtil;
import consulo.xml.psi.xml.XmlFile;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.model.resolve.RelaxIncludeIndex;
import org.kohsuke.rngom.ast.builder.BuildException;
import org.kohsuke.rngom.ast.builder.IncludedGrammar;
import org.kohsuke.rngom.ast.builder.SchemaBuilder;
import org.kohsuke.rngom.ast.om.ParsedPattern;
import org.kohsuke.rngom.binary.SchemaBuilderImpl;
import org.kohsuke.rngom.binary.SchemaPatternBuilder;
import org.kohsuke.rngom.digested.DPattern;
import org.kohsuke.rngom.digested.DSchemaBuilderImpl;
import org.kohsuke.rngom.dt.CachedDatatypeLibraryFactory;
import org.kohsuke.rngom.dt.CascadingDatatypeLibraryFactory;
import org.kohsuke.rngom.dt.DoNothingDatatypeLibraryFactoryImpl;
import org.kohsuke.rngom.dt.builtin.BuiltinDatatypeLibraryFactory;
import org.kohsuke.rngom.parse.IllegalSchemaException;
import org.kohsuke.rngom.parse.Parseable;
import org.kohsuke.rngom.parse.compact.CompactParseable;
import org.kohsuke.rngom.parse.xml.SAXParseable;
import org.relaxng.datatype.DatatypeLibrary;
import org.relaxng.datatype.DatatypeLibraryFactory;
import org.relaxng.datatype.helpers.DatatypeLibraryLoader;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.Nonnull;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.util.concurrent.ConcurrentMap;

/*
* Created by IntelliJ IDEA.
* User: sweinreuter
* Date: 19.07.2007
*/
public class RngParser
{
	private static final Logger LOG = Logger.getInstance("#RngParser");

	private static final NotNullLazyValue<DatatypeLibraryFactory> DT_LIBRARY_FACTORY = new AtomicNotNullLazyValue<DatatypeLibraryFactory>()
	{
		@Nonnull
		@Override
		protected DatatypeLibraryFactory compute()
		{
			return new BuiltinDatatypeLibraryFactory(new CachedDatatypeLibraryFactory(new CascadingDatatypeLibraryFactory(createXsdDatatypeFactory(), new DatatypeLibraryLoader()))
			{
				@Override
				public synchronized DatatypeLibrary createDatatypeLibrary(String namespaceURI)
				{
					return super.createDatatypeLibrary(namespaceURI);
				}
			});
		}
	};

	private static final ConcurrentMap<String, DPattern> ourCache = ContainerUtil.createConcurrentSoftMap();

	private static DatatypeLibraryFactory createXsdDatatypeFactory()
	{
		try
		{
			return new DatatypeLibraryFactoryImpl();
		}
		catch(Throwable e)
		{
			LOG.error("Could not create DT library implementation 'com.thaiopensource.datatype.xsd.DatatypeLibraryFactoryImpl'. Plugin's classpath seems to be broken.", e);
			return new DoNothingDatatypeLibraryFactoryImpl();
		}
	}

	static final Key<CachedValue<Schema>> SCHEMA_KEY = Key.create("SCHEMA");

	public static final DefaultHandler DEFAULT_HANDLER = new DefaultHandler()
	{
		@Override
		public void error(SAXParseException e) throws SAXException
		{
			LOG.info("e.getMessage() = " + e.getMessage() + " [" + e.getSystemId() + "]");
			LOG.info(e);
		}
	};

	static final PropertyMap EMPTY_PROPS = new PropertyMapBuilder().toPropertyMap();

	public static DPattern getCachedPattern(final PsiFile descriptorFile, final ErrorHandler eh)
	{
		final VirtualFile file = descriptorFile.getVirtualFile();

		if(file == null)
		{
			return parsePattern(descriptorFile, eh, false);
		}
		String url = file.getUrl();
		DPattern pattern = ourCache.get(url);
		if(pattern == null)
		{
			pattern = parsePattern(descriptorFile, eh, false);
		}
		if(pattern != null)
		{
			DPattern oldPattern = ourCache.putIfAbsent(url, pattern);
			if(oldPattern != null)
			{
				return oldPattern;
			}
		}
		return pattern;
	}

	public static DPattern parsePattern(final PsiFile file, final ErrorHandler eh, boolean checking)
	{
		try
		{
			final Parseable p = createParsable(file, eh);
			if(checking)
			{
				p.parse(new SchemaBuilderImpl(eh, DT_LIBRARY_FACTORY.getValue(), new SchemaPatternBuilder()));
			}
			else
			{
				return p.parse(new DSchemaBuilderImpl());
			}
		}
		catch(BuildException e)
		{
			LOG.info(e);
		}
		catch(IllegalSchemaException e)
		{
			final VirtualFile virtualFile = file.getVirtualFile();
			if(virtualFile != null)
			{
				if(LOG.isDebugEnabled())
				{
					LOG.debug("invalid schema: " + virtualFile.getPresentableUrl(), e);
				}
				else
				{
					LOG.info("invalid schema: " + virtualFile.getPresentableUrl() + ". [" + e.getMessage() + "]");
				}
			}
		}
		return null;
	}

	private static Parseable createParsable(final PsiFile file, final ErrorHandler eh)
	{
		final InputSource source = makeInputSource(file);
		final VirtualFile virtualFile = file.getVirtualFile();

		if(file.getFileType() == RncFileType.getInstance())
		{
			return new CompactParseable(source, eh)
			{
				@Override
				public ParsedPattern parseInclude(String uri, SchemaBuilder schemaBuilder, IncludedGrammar g, String inheritedNs) throws BuildException, IllegalSchemaException
				{
					ProgressManager.checkCanceled();
					return super.parseInclude(resolveURI(virtualFile, uri), schemaBuilder, g, inheritedNs);
				}
			};
		}
		else
		{
			return new SAXParseable(source, eh)
			{
				@Override
				public ParsedPattern parseInclude(String uri, SchemaBuilder schemaBuilder, IncludedGrammar g, String inheritedNs) throws BuildException, IllegalSchemaException
				{
					ProgressManager.checkCanceled();
					return super.parseInclude(resolveURI(virtualFile, uri), schemaBuilder, g, inheritedNs);
				}
			};
		}
	}

	private static String resolveURI(VirtualFile descriptorFile, String s)
	{
		final VirtualFile file = UriUtil.findRelativeFile(s, descriptorFile);
		if(file != null)
		{
			s = VirtualFileUtil.fixIDEAUrl(file.getUrl());
		}
		return s;
	}

	public static Schema getCachedSchema(final XmlFile descriptorFile)
	{
		CachedValue<Schema> value = descriptorFile.getUserData(SCHEMA_KEY);
		if(value == null)
		{
			final CachedValueProvider<Schema> provider = () ->
			{
				final InputSource inputSource = makeInputSource(descriptorFile);

				try
				{
					final Schema schema = new MySchemaReader(descriptorFile).createSchema(inputSource, EMPTY_PROPS);
					final PsiElementProcessor.CollectElements<XmlFile> processor = new PsiElementProcessor.CollectElements<>();
					RelaxIncludeIndex.processForwardDependencies(descriptorFile, processor);
					if(processor.getCollection().size() > 0)
					{
						return CachedValueProvider.Result.create(schema, processor.toArray(), descriptorFile);
					}
					else
					{
						return CachedValueProvider.Result.createSingleDependency(schema, descriptorFile);
					}
				}
				catch(Exception e)
				{
					LOG.info(e);
					return CachedValueProvider.Result.createSingleDependency(null, descriptorFile);
				}
			};

			final CachedValuesManager mgr = CachedValuesManager.getManager(descriptorFile.getProject());
			value = mgr.createCachedValue(provider, false);
			descriptorFile.putUserData(SCHEMA_KEY, value);
		}
		return value.getValue();
	}

	private static InputSource makeInputSource(PsiFile descriptorFile)
	{
		final InputSource inputSource = new InputSource(new StringReader(descriptorFile.getText()));
		final VirtualFile file = descriptorFile.getVirtualFile();
		if(file != null)
		{
			inputSource.setSystemId(VirtualFileUtil.fixIDEAUrl(file.getUrl()));
		}
		return inputSource;
	}

	static class MySchemaReader extends SchemaReaderImpl
	{
		private final PsiFile myDescriptorFile;

		public MySchemaReader(PsiFile descriptorFile)
		{
			myDescriptorFile = descriptorFile;
		}

		@Override
		protected com.thaiopensource.relaxng.parse.Parseable<Pattern, NameClass, Locator, VoidValue, CommentListImpl, AnnotationsImpl> createParseable(SAXSource source,
				SAXResolver resolver,
				ErrorHandler errorHandler,
				PropertyMap properties) throws SAXException
		{
			Input inputSource = SAX.createInput(source.getInputSource());
			if(myDescriptorFile.getFileType() == RncFileType.getInstance())
			{
				return new com.thaiopensource.relaxng.parse.compact.CompactParseable<>(inputSource, resolver.getResolver(), errorHandler);
			}
			else
			{
				return new com.thaiopensource.relaxng.parse.sax.SAXParseable<>(source, resolver, errorHandler);
			}
		}
	}
}