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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlMarkupDecl;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.dtd.XmlNSDescriptorImpl;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.util.PsiTreeUtil;

import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public class DtdResolveUtil
{
	@Nullable
	static XmlNSDescriptor getNsDescriptor(XmlElement element)
	{
		final XmlElement parentThatProvidesMetaData = PsiTreeUtil.getParentOfType(CompletionUtilCore.getOriginalElement(element), XmlDocument.class, XmlMarkupDecl.class);

		if(parentThatProvidesMetaData instanceof XmlDocument)
		{
			final XmlDocument document = (XmlDocument) parentThatProvidesMetaData;
			XmlNSDescriptor rootTagNSDescriptor = document.getRootTagNSDescriptor();
			if(rootTagNSDescriptor == null)
			{
				rootTagNSDescriptor = (XmlNSDescriptor) document.getMetaData();
			}
			return rootTagNSDescriptor;
		}
		else if(parentThatProvidesMetaData instanceof XmlMarkupDecl)
		{
			final XmlMarkupDecl markupDecl = (XmlMarkupDecl) parentThatProvidesMetaData;
			final PsiMetaData psiMetaData = markupDecl.getMetaData();

			if(psiMetaData instanceof XmlNSDescriptor)
			{
				return (XmlNSDescriptor) psiMetaData;
			}
		}

		return null;
	}

	@Nullable
	public static XmlElementDescriptor resolveElementReference(String name, XmlElement context)
	{
		XmlNSDescriptor rootTagNSDescriptor = getNsDescriptor(context);

		if(rootTagNSDescriptor instanceof XmlNSDescriptorImpl)
		{
			return ((XmlNSDescriptorImpl) rootTagNSDescriptor).getElementDescriptor(name);
		}
		return null;
	}
}
