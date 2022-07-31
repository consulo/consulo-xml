/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.xml.codeInsight.completion;

import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.CharsetToolkit;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.xml.patterns.XmlPatterns;
import consulo.xml.psi.html.HtmlTag;
import consulo.xml.psi.impl.source.html.dtd.HtmlElementDescriptorImpl;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;

import static consulo.language.pattern.PlatformPatterns.psiElement;

@ExtensionImpl(id = "html")
public class HtmlCompletionContributor extends CompletionContributor
{
	public HtmlCompletionContributor()
	{
		extend(CompletionType.BASIC, psiElement().inside(XmlPatterns.xmlAttributeValue()), new CompletionProvider()
		{
			@Override
			public void addCompletions(@Nonnull CompletionParameters parameters, ProcessingContext context, @Nonnull CompletionResultSet result)
			{
				final PsiElement position = parameters.getPosition();
				if(!hasHtmlAttributesCompletion(position))
				{
					return;
				}
				final XmlAttributeValue attributeValue = PsiTreeUtil.getParentOfType(position, XmlAttributeValue.class, false);
				if(attributeValue != null && attributeValue.getParent() instanceof XmlAttribute)
				{
					for(String element : addSpecificCompletions((XmlAttribute) attributeValue.getParent()))
					{
						result.addElement(LookupElementBuilder.create(element));
					}
				}
			}
		});
	}

	private static boolean hasHtmlAttributesCompletion(PsiElement position)
	{
		if(PsiTreeUtil.getParentOfType(position, HtmlTag.class, false) != null)
		{
			return true;
		}
		XmlTag xmlTag = PsiTreeUtil.getParentOfType(position, XmlTag.class, false);
		return xmlTag != null && xmlTag.getLanguage() == XHTMLLanguage.INSTANCE;
	}

	@Nonnull
	@NonNls
	protected static String[] addSpecificCompletions(final XmlAttribute attribute)
	{
		@NonNls String name = attribute.getName();
		final XmlTag tag = attribute.getParent();
		if(tag == null)
		{
			return ArrayUtil.EMPTY_STRING_ARRAY;
		}

		@NonNls String tagName = tag.getName();
		if(tag.getDescriptor() instanceof HtmlElementDescriptorImpl)
		{
			name = name.toLowerCase();
			tagName = tagName.toLowerCase();
		}

		final String namespace = tag.getNamespace();
		if(XmlUtil.XHTML_URI.equals(namespace) || XmlUtil.HTML_URI.equals(namespace))
		{

			if("target".equals(name))
			{
				return new String[]{
						"_blank",
						"_top",
						"_self",
						"_parent"
				};
			}
			else if("enctype".equals(name))
			{
				return new String[]{
						"multipart/form-data",
						"application/x-www-form-urlencoded"
				};
			}
			else if("rel".equals(name) || "rev".equals(name))
			{
				return new String[]{
						"alternate",
						"author",
						"bookmark",
						"help",
						"icon",
						"license",
						"next",
						"nofollow",
						"noreferrer",
						"prefetch",
						"prev",
						"search",
						"stylesheet",
						"tag",
						"start",
						"contents",
						"index",
						"glossary",
						"copyright",
						"chapter",
						"section",
						"subsection",
						"appendix",
						"script",
						"import",
						"apple-touch-icon",
						"apple-touch-icon-precomposed",
						"apple-touch-startup-image"
				};
			}
			else if("media".equals(name))
			{
				return new String[]{
						"all",
						"braille",
						"embossed",
						"handheld",
						"print",
						"projection",
						"screen",
						"speech",
						"tty",
						"tv"
				};
			}
			else if("language".equals(name))
			{
				return new String[]{
						"JavaScript",
						"VBScript",
						"JScript",
						"JavaScript1.2",
						"JavaScript1.3",
						"JavaScript1.4",
						"JavaScript1.5"
				};
			}
			else if("type".equals(name) && "link".equals(tagName))
			{
				return new String[]{
						"text/css",
						"text/html",
						"text/plain",
						"text/xml"
				};
			}
			else if("http-equiv".equals(name) && "meta".equals(tagName))
			{
				return HtmlUtil.RFC2616_HEADERS;
			}
			else if("content".equals(name) && "meta".equals(tagName) && tag.getAttribute("name") == null)
			{
				return HtmlUtil.CONTENT_TYPES;
			}
			else if("accept".equals(name) && "input".equals(tagName))
			{
				return HtmlUtil.CONTENT_TYPES;
			}
			else if("accept-charset".equals(name) || "charset".equals(name))
			{
				Charset[] charSets = CharsetToolkit.getAvailableCharsets();
				String[] names = new String[charSets.length];
				for(int i = 0; i < names.length; i++)
				{
					names[i] = charSets[i].toString();
				}
				return names;
			}
		}

		return ArrayUtil.EMPTY_STRING_ARRAY;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return Language.ANY;
	}
}
