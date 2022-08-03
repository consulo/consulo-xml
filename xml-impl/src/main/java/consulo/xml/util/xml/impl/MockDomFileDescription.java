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
package consulo.xml.util.xml.impl;

import javax.annotation.Nonnull;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomFileDescription;

/**
 * @author peter
 */
public class MockDomFileDescription<T> extends DomFileDescription<T>
{
	private final XmlFile myFile;

	public MockDomFileDescription(final Class<T> aClass, final String rootTagName, final XmlFile file)
	{
		super(aClass, rootTagName);
		myFile = file;
	}

	@Override
	public boolean isMyFile(@Nonnull final XmlFile xmlFile)
	{
		return myFile == xmlFile;
	}

	@Override
	public boolean acceptsOtherRootTagNames()
	{
		return true;
	}

	@Override
	public boolean isAutomaticHighlightingEnabled()
	{
		return false;
	}
}
