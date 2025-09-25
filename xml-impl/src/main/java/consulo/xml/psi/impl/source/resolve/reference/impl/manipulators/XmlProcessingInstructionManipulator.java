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
package consulo.xml.psi.impl.source.resolve.reference.impl.manipulators;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.util.IncorrectOperationException;
import consulo.xml.psi.xml.XmlProcessingInstruction;
import consulo.xml.psi.xml.XmlTokenType;

import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2013-02-20
 */
@ExtensionImpl
public class XmlProcessingInstructionManipulator extends AbstractElementManipulator<XmlProcessingInstruction>
{
	@Override
	public XmlProcessingInstruction handleContentChange(XmlProcessingInstruction element, TextRange range, String newContent) throws IncorrectOperationException
	{
		return XmlAttributeValueManipulator.handleContentChange(element, range, newContent, XmlTokenType.XML_TAG_CHARACTERS);
	}

	@Nonnull
	@Override
	public Class<XmlProcessingInstruction> getElementClass()
	{
		return XmlProcessingInstruction.class;
	}
}
