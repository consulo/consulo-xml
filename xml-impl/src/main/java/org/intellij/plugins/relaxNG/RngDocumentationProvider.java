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

package org.intellij.plugins.relaxNG;

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import org.intellij.plugins.relaxNG.model.descriptors.CompositeDescriptor;
import org.intellij.plugins.relaxNG.model.descriptors.RngElementDescriptor;
import org.intellij.plugins.relaxNG.model.descriptors.RngXmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.kohsuke.rngom.digested.DElementPattern;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/*
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 19.11.2007
 */
@ExtensionImpl(id = "rng")
public class RngDocumentationProvider implements LanguageDocumentationProvider
{
	private static final Logger LOG = Logger.getInstance(RngDocumentationProvider.class);

	@NonNls
	private static final String COMPATIBILITY_ANNOTATIONS_1_0 = "http://relaxng.org/ns/compatibility/annotations/1.0";

	@Override
	@Nullable
	public String generateDoc(PsiElement element, @Nullable PsiElement originalElement)
	{
		final XmlElement c = PsiTreeUtil.getParentOfType(originalElement, XmlTag.class, XmlAttribute.class);
		if(c != null && c.getManager() == null)
		{
			LOG.warn("Invalid context element passed to generateDoc()", new Throwable("<stack trace>"));
			return null;
		}
		if(c instanceof XmlTag)
		{
			final XmlTag xmlElement = (XmlTag) c;
			final XmlElementDescriptor descriptor = xmlElement.getDescriptor();
			if(descriptor instanceof CompositeDescriptor)
			{
				final StringBuilder sb = new StringBuilder();
				final CompositeDescriptor d = (CompositeDescriptor) descriptor;
				final DElementPattern[] patterns = d.getElementPatterns();
				final Set<PsiElement> elements = ContainerUtil.newIdentityTroveSet();
				for(DElementPattern pattern : patterns)
				{
					final PsiElement psiElement = d.getDeclaration(pattern.getLocation());
					if(psiElement instanceof XmlTag && elements.add(psiElement))
					{
						if(sb.length() > 0)
						{
							sb.append("<hr>");
						}
						sb.append(getDocumentationFromTag((XmlTag) psiElement, xmlElement.getLocalName(), "Element"));
					}
				}
				return makeDocumentation(sb);
			}
			else if(descriptor instanceof RngElementDescriptor)
			{
				final RngElementDescriptor d = (RngElementDescriptor) descriptor;
				final PsiElement declaration = d.getDeclaration();
				if(declaration instanceof XmlTag)
				{
					return makeDocumentation(getDocumentationFromTag((XmlTag) declaration, xmlElement.getLocalName(), "Element"));
				}
			}
		}
		else if(c instanceof XmlAttribute)
		{
			final XmlAttribute attribute = (XmlAttribute) c;
			final XmlAttributeDescriptor descriptor = attribute.getDescriptor();
			if(descriptor instanceof RngXmlAttributeDescriptor)
			{
				final RngXmlAttributeDescriptor d = (RngXmlAttributeDescriptor) descriptor;
				final StringBuilder sb = new StringBuilder();
				final Collection<PsiElement> declaration = ContainerUtil.newIdentityTroveSet(d.getDeclarations());
				for(PsiElement psiElement : declaration)
				{
					if(psiElement instanceof XmlTag)
					{
						if(sb.length() > 0)
						{
							sb.append("<hr>");
						}
						sb.append(getDocumentationFromTag((XmlTag) psiElement, d.getName(), "Attribute"));
					}
				}
			}
		}
		else if(element instanceof XmlTag)
		{
			return makeDocumentation(getDocumentationFromTag((XmlTag) element, ((XmlTag) element).getLocalName(), "Element"));
		}
		return null;
	}

	private static String makeDocumentation(StringBuilder sb)
	{
		if(sb == null)
		{
			return null;
		}
		String s = sb.toString().replaceAll("\n", "<br>");
		if(!s.startsWith("<html>"))
		{
			s = XmlStringUtil.wrapInHtml(s);
		}
		return s;
	}

	private static StringBuilder getDocumentationFromTag(XmlTag tag, String localName, String kind)
	{
		if(tag.getNamespace().equals(ApplicationLoader.RNG_NAMESPACE))
		{
			final StringBuilder sb = new StringBuilder();
			sb.append(kind).append(": <b>").append(localName).append("</b><br>");
			final XmlTag[] docTags = tag.findSubTags("documentation", COMPATIBILITY_ANNOTATIONS_1_0);
			for(XmlTag docTag : docTags)
			{
				sb.append(docTag.getValue().getTrimmedText());
				sb.append("\n");
			}
			final XmlTag nextTag = PsiTreeUtil.getNextSiblingOfType(tag, XmlTag.class);
			if(nextTag != null)
			{
				if("documentation".equals(nextTag.getLocalName()) && COMPATIBILITY_ANNOTATIONS_1_0.equals(nextTag.getNamespace()))
				{
					sb.append(nextTag.getValue().getTrimmedText());
				}
			}
			return sb;
		}
		return null;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}