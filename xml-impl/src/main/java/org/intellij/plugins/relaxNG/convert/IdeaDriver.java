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

package org.intellij.plugins.relaxNG.convert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import com.thaiopensource.relaxng.edit.SchemaCollection;
import com.thaiopensource.relaxng.input.InputFailedException;
import com.thaiopensource.relaxng.input.InputFormat;
import com.thaiopensource.relaxng.input.MultiInputFormat;
import com.thaiopensource.relaxng.input.dtd.DtdInputFormat;
import com.thaiopensource.relaxng.input.parse.compact.CompactParseInputFormat;
import com.thaiopensource.relaxng.input.parse.sax.SAXParseInputFormat;
import com.thaiopensource.relaxng.input.xml.XmlInputFormat;
import com.thaiopensource.relaxng.output.LocalOutputDirectory;
import com.thaiopensource.relaxng.output.OutputDirectory;
import com.thaiopensource.relaxng.output.OutputFailedException;
import com.thaiopensource.relaxng.output.OutputFormat;
import com.thaiopensource.relaxng.output.dtd.DtdOutputFormat;
import com.thaiopensource.relaxng.output.rnc.RncOutputFormat;
import com.thaiopensource.relaxng.output.rng.RngOutputFormat;
import com.thaiopensource.relaxng.output.xsd.XsdOutputFormat;
import com.thaiopensource.relaxng.translate.util.InvalidParamsException;
import com.thaiopensource.resolver.BasicResolver;
import com.thaiopensource.resolver.Resolver;
import com.thaiopensource.util.UriOrFile;

public class IdeaDriver
{

	private static final int DEFAULT_LINE_LENGTH = 72;
	private static final int DEFAULT_INDENT = 2;

	private final ConvertSchemaSettings settings;
	private final Project myProject;

	public IdeaDriver(ConvertSchemaSettings settings, Project project)
	{
		this.settings = settings;
		myProject = project;
	}

	@SuppressWarnings({"ThrowableInstanceNeverThrown"})
	public void convert(SchemaType inputType, IdeaErrorHandler errorHandler, VirtualFile... inputFiles)
	{
		if(inputFiles.length == 0)
		{
			throw new IllegalArgumentException();
		}

		try
		{
			final InputFormat inFormat = getInputFormat(inputType);
			if(inputFiles.length > 1)
			{
				if(!(inFormat instanceof MultiInputFormat))
				{
					throw new IllegalArgumentException();
				}
			}

			final VirtualFile inputFile = inputFiles[0];
			final SchemaType type = settings.getOutputType();
			final String outputType = type.toString().toLowerCase();

			final ArrayList<String> inputParams = new ArrayList<>();

			if(inputType != SchemaType.DTD)
			{
				final Charset charset = inputFile.getCharset();
				inputParams.add("encoding=" + charset.name());
			}

			final ArrayList<String> outputParams = new ArrayList<>();
			settings.addAdvancedSettings(inputParams, outputParams);

			//      System.out.println("INPUT: " + inputParams);
			//      System.out.println("OUTPUT: " + outputParams);

			Resolver catalogResolver = BasicResolver.getInstance();
			final SchemaCollection sc;
			final String input = inputFile.getPath();
			final String uri = UriOrFile.toUri(input);
			try
			{
				if(inFormat instanceof MultiInputFormat)
				{
					final MultiInputFormat format = (MultiInputFormat) inFormat;
					final String[] uris = new String[inputFiles.length];
					for(int i = 0; i < inputFiles.length; i++)
					{
						uris[i] = UriOrFile.toUri(inputFiles[i].getPath());
					}
					sc = format.load(uris, ArrayUtil.toStringArray(inputParams), outputType, errorHandler, catalogResolver);
				}
				else
				{
					sc = inFormat.load(uri, ArrayUtil.toStringArray(inputParams), outputType, errorHandler, catalogResolver);
				}
			}
			catch(IOException e)
			{
				errorHandler.fatalError(new SAXParseException(e.getMessage(), null, uri, -1, -1, e));
				return;
			}

			final File destination = new File(settings.getOutputDestination());
			final File outputFile;
			if(destination.isDirectory())
			{
				final String name = new File(input).getName();
				final int ext = name.lastIndexOf('.');
				outputFile = new File(destination, (ext > 0 ? name.substring(0, ext) : name) + "." + outputType);
			}
			else
			{
				outputFile = destination;
			}

			try
			{
				final int indent = settings.getIndent();
				final int length = settings.getLineLength();
				final OutputDirectory od = new LocalOutputDirectory(sc.getMainUri(), outputFile, "." + outputType, settings.getOutputEncoding(), length > 0 ? length : DEFAULT_LINE_LENGTH, indent > 0
						? indent : DEFAULT_INDENT)
				{
					@Override
					public Stream open(String sourceUri, String encoding) throws IOException
					{
						final String s = reference(null, sourceUri);
						final File file = new File(outputFile.getParentFile(), s);
						if(file.exists())
						{
							final String msg = "The file '" + file.getAbsolutePath() + "' already exists. Overwrite it?";
							final int choice = Messages.showYesNoDialog(myProject, msg, "Output File Exists", Messages.getWarningIcon());
							if(choice == Messages.YES)
							{
								return super.open(sourceUri, encoding);
							}
							else if(choice == 1)
							{
								throw new CanceledException();
							}
						}
						return super.open(sourceUri, encoding);
					}
				};

				final OutputFormat of = getOutputFormat(settings.getOutputType());

				of.output(sc, od, ArrayUtil.toStringArray(outputParams), inputType.toString().toLowerCase(), errorHandler);
			}
			catch(IOException e)
			{
				errorHandler.fatalError(new SAXParseException(e.getMessage(), null, UriOrFile.fileToUri(outputFile), -1, -1, e));
			}
		}
		catch(CanceledException | InvalidParamsException | InputFailedException e)
		{
			// user abort
		}
		catch(SAXParseException e)
		{
			errorHandler.error(e);
		}
		catch(OutputFailedException e)
		{
			// handled by ErrorHandler
		}
		catch(SAXException e)
		{
			// cannot happen or is already handled
		}
	}


	private OutputFormat getOutputFormat(SchemaType outputType)
	{
		switch(outputType)
		{
			case DTD:
				return new DtdOutputFormat();
			case RNC:
				return new RncOutputFormat();
			case RNG:
				return new RngOutputFormat();
			case XSD:
				return new XsdOutputFormat();
			default:
				assert false : "Unsupported output type: " + outputType;
				return null;
		}
	}

	private InputFormat getInputFormat(SchemaType type)
	{
		switch(type)
		{
			case DTD:
				return new DtdInputFormat();
			case RNC:
				return new CompactParseInputFormat();
			case RNG:
				return new SAXParseInputFormat();
			case XML:
				return new XmlInputFormat();
			default:
				assert false : "Unsupported input type: " + type;
				return null;
		}
	}

	private static class CanceledException extends RuntimeException
	{
	}
}