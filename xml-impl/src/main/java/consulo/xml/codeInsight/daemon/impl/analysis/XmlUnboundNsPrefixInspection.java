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
package consulo.xml.codeInsight.daemon.impl.analysis;

import com.intellij.xml.XmlBundle;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import com.intellij.xml.util.XmlTagUtil;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.highlight.HighlightLevelUtil;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.xml.codeInspection.XmlSuppressableInspectionTool;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.*;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class XmlUnboundNsPrefixInspection extends XmlSuppressableInspectionTool
{
	private static final String XML = "xml";

	@Nullable
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}

	@Nonnull
	@Override
	public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, final boolean isOnTheFly)
	{
		return new XmlElementVisitor()
		{

			private Boolean isXml;

			private boolean isXmlFile(XmlElement element)
			{
				if(isXml == null)
				{
					final PsiFile file = element.getContainingFile();
					isXml = file instanceof XmlFile && !InjectedLanguageManager.getInstance(element.getProject()).isInjectedFragment(file);
				}
				return isXml.booleanValue();
			}

			@Override
			public void visitXmlToken(final XmlToken token)
			{
				if(isXmlFile(token) && token.getTokenType() == XmlTokenType.XML_NAME)
				{
					PsiElement element = token.getPrevSibling();
					while(element instanceof PsiWhiteSpace)
					{
						element = element.getPrevSibling();
					}

					if(element instanceof XmlToken && ((XmlToken) element).getTokenType() == XmlTokenType.XML_START_TAG_START)
					{
						PsiElement parent = element.getParent();

						if(parent instanceof XmlTag && !(token.getNextSibling() instanceof OuterLanguageElement))
						{
							XmlTag tag = (XmlTag) parent;
							checkUnboundNamespacePrefix(tag, tag, tag.getNamespacePrefix(), token, holder, isOnTheFly);
						}
					}
				}
			}

			@Override
			public void visitXmlAttribute(final XmlAttribute attribute)
			{
				if(!isXmlFile(attribute))
				{
					return;
				}
				final String namespace = attribute.getNamespace();
				if(attribute.isNamespaceDeclaration() || XmlUtil.XML_SCHEMA_INSTANCE_URI.equals(namespace))
				{
					return;
				}

				XmlTag tag = attribute.getParent();
				XmlElementDescriptor elementDescriptor = tag.getDescriptor();
				if(elementDescriptor == null ||
						elementDescriptor instanceof AnyXmlElementDescriptor)
				{
					return;
				}


				final String name = attribute.getName();

				checkUnboundNamespacePrefix(attribute, tag, XmlUtil.findPrefixByQualifiedName(name), null, holder, isOnTheFly);
			}

			@Override
			public void visitXmlAttributeValue(XmlAttributeValue value)
			{
				PsiReference[] references = value.getReferences();
				for(PsiReference reference : references)
				{
					if(reference instanceof SchemaPrefixReference schemaPrefixReference)
					{
						if(!XML.equals(schemaPrefixReference.getNamespacePrefix()) && reference.resolve() == null)
						{
							holder.registerProblem(
								reference,
								XmlErrorLocalize.unboundNamespace(schemaPrefixReference.getNamespacePrefix()).get(),
								ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
							);
						}
					}
				}
			}
		};
	}

	private static void checkUnboundNamespacePrefix(
		final XmlElement element,
		final XmlTag context,
		String namespacePrefix,
		final XmlToken token,
		final ProblemsHolder holder,
		boolean isOnTheFly
	)
	{

		if(namespacePrefix.length() == 0 && (!(element instanceof XmlTag) || !(element.getParent() instanceof XmlDocument))
				|| XML.equals(namespacePrefix))
		{
			return;
		}

		final String namespaceByPrefix = context.getNamespaceByPrefix(namespacePrefix);
		if(namespaceByPrefix.length() != 0)
		{
			return;
		}
		PsiFile psiFile = context.getContainingFile();
		if(!(psiFile instanceof XmlFile))
		{
			return;
		}
		final XmlFile containingFile = (XmlFile) psiFile;
		if(!HighlightLevelUtil.shouldInspect(containingFile))
		{
			return;
		}

		final XmlExtension extension = XmlExtension.getExtension(containingFile);
		if(extension.getPrefixDeclaration(context, namespacePrefix) != null)
		{
			return;
		}

		final LocalizeValue localizedMessage = isOnTheFly
			? XmlErrorLocalize.unboundNamespace(namespacePrefix)
			: XmlErrorLocalize.unboundNamespaceNoParam();

		if(namespacePrefix.length() == 0)
		{
			final XmlTag tag = (XmlTag) element;
			if(!XmlUtil.JSP_URI.equals(tag.getNamespace()))
			{
				reportTagProblem(tag, localizedMessage.get(), null, ProblemHighlightType.INFORMATION,
						isOnTheFly ? new CreateNSDeclarationIntentionFix(context, namespacePrefix, token) : null,
						holder);
			}
			return;
		}

		final int prefixLength = namespacePrefix.length();
		final TextRange range = new TextRange(0, prefixLength);
		final HighlightInfoType infoType = extension.getHighlightInfoType(containingFile);
		final ProblemHighlightType highlightType = infoType == HighlightInfoType.ERROR ? ProblemHighlightType.ERROR : ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;
		if(element instanceof XmlTag)
		{
			final CreateNSDeclarationIntentionFix fix = isOnTheFly ? new CreateNSDeclarationIntentionFix(context, namespacePrefix, token) : null;
			reportTagProblem(element, localizedMessage.get(), range, highlightType, fix, holder);
		}
		else
		{
			holder.registerProblem(element, localizedMessage.get(), highlightType, range);
		}
	}

	private static void reportTagProblem(
		final XmlElement element,
		final String localizedMessage,
		final TextRange range,
		final ProblemHighlightType highlightType,
		final CreateNSDeclarationIntentionFix fix,
		final ProblemsHolder holder
	)
	{

		XmlToken nameToken = XmlTagUtil.getStartTagNameElement((XmlTag) element);
		if(nameToken != null)
		{
			holder.registerProblem(nameToken, localizedMessage, highlightType, range, fix);
		}
		nameToken = XmlTagUtil.getEndTagNameElement((XmlTag) element);
		if(nameToken != null)
		{
			holder.registerProblem(nameToken, localizedMessage, highlightType, range, fix);
		}
	}


	@Nonnull
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.WARNING;
	}

	public boolean isEnabledByDefault()
	{
		return true;
	}

	@Nonnull
	public String getGroupDisplayName()
	{
		return XmlBundle.message("xml.inspections.group.name");
	}

	@Nonnull
	public String getDisplayName()
	{
		return XmlBundle.message("xml.inspections.unbound.prefix");
	}

	@Nonnull
	@NonNls
	public String getShortName()
	{
		return "XmlUnboundNsPrefix";
	}
}

