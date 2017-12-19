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
package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.util.XmlUtil;

/**
 * @author Dmitry Avdeev
 */
public class XmlBaseReferenceProvider extends PsiReferenceProvider
{

	private final boolean myAcceptSelf;

	public XmlBaseReferenceProvider(boolean acceptSelf)
	{
		myAcceptSelf = acceptSelf;
	}

	@NotNull
	@Override
	public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull ProcessingContext context)
	{
		PsiReference reference = URIReferenceProvider.getUrlReference(element, ElementManipulators.getValueText(element));
		if(reference != null)
		{
			return new PsiReference[]{reference};
		}
		FileReferenceSet referenceSet = FileReferenceSet.createSet(element, false, false, false);
		referenceSet.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, file -> getContext(element, file));
		return referenceSet.getAllReferences();
	}

	private Collection<PsiFileSystemItem> getContext(PsiElement element, PsiFile file)
	{
		XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
		if(!myAcceptSelf && tag != null)
		{
			tag = tag.getParentTag();
		}
		while(tag != null)
		{
			XmlAttribute base = tag.getAttribute("base", XmlUtil.XML_NAMESPACE_URI);
			if(base != null)
			{
				XmlAttributeValue value = base.getValueElement();
				if(value != null)
				{
					PsiReference reference = value.getReference();
					if(reference instanceof PsiPolyVariantReference)
					{
						ResolveResult[] results = ((PsiPolyVariantReference) reference).multiResolve(false);
						return ContainerUtil.map(results, result -> (PsiFileSystemItem) result.getElement());
					}
				}
			}
			tag = tag.getParentTag();
		}
		PsiDirectory directory = file.getContainingDirectory();
		return directory == null ? Collections.<PsiFileSystemItem>emptyList() : Collections.<PsiFileSystemItem>singletonList(directory);
	}
}
