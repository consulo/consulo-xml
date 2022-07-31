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
package consulo.xml.psi.impl.source.xml;

import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.XmlNamespaceHelper;
import com.intellij.xml.XmlTagNameProvider;
import com.intellij.xml.index.XmlNamespaceIndex;
import com.intellij.xml.index.XsdNamespaceBuilder;
import com.intellij.xml.util.XmlUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.application.util.function.Processors;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.lookup.*;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.codeInsight.completion.XmlTagInsertHandler;
import consulo.xml.psi.impl.source.html.dtd.HtmlElementDescriptorImpl;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;
import java.util.*;

@ExtensionImpl
public class DefaultXmlTagNameProvider implements XmlTagNameProvider
{
	private static final Logger LOG = Logger.getInstance(DefaultXmlTagNameProvider.class);

	@Override
	public void addTagNameVariants(List<LookupElement> elements, @Nonnull XmlTag tag, String prefix)
	{
		final List<String> namespaces;
		if(prefix.isEmpty())
		{
			namespaces = new ArrayList<>(Arrays.asList(tag.knownNamespaces()));
			namespaces.add(XmlUtil.EMPTY_URI); // empty namespace
		}
		else
		{
			namespaces = new ArrayList<>(Collections.singletonList(tag.getNamespace()));
		}
		PsiFile psiFile = tag.getContainingFile();
		XmlExtension xmlExtension = XmlExtension.getExtension(psiFile);
		List<String> nsInfo = new ArrayList<>();
		List<XmlElementDescriptor> variants = TagNameVariantCollector.getTagDescriptors(tag, namespaces, nsInfo);

		if(variants.isEmpty() && psiFile instanceof XmlFile && ((XmlFile) psiFile).getRootTag() == tag)
		{
			getRootTagsVariants(tag, elements);
			return;
		}

		final Set<String> visited = new HashSet<>();
		for(int i = 0; i < variants.size(); i++)
		{
			XmlElementDescriptor descriptor = variants.get(i);
			String qname = descriptor.getName(tag);
			if(!visited.add(qname))
			{
				continue;
			}
			if(!prefix.isEmpty() && qname.startsWith(prefix + ":"))
			{
				qname = qname.substring(prefix.length() + 1);
			}

			PsiElement declaration = descriptor.getDeclaration();
			if(declaration != null && !declaration.isValid())
			{
				LOG.error(descriptor + " contains invalid declaration: " + declaration);
			}
			LookupElementBuilder lookupElement = declaration == null ? LookupElementBuilder.create(qname) : LookupElementBuilder.create(declaration, qname);
			final int separator = qname.indexOf(':');
			if(separator > 0)
			{
				lookupElement = lookupElement.withLookupString(qname.substring(separator + 1));
			}
			String ns = nsInfo.get(i);
			if(StringUtil.isNotEmpty(ns))
			{
				lookupElement = lookupElement.withTypeText(ns, true);
			}
			if(descriptor instanceof PsiPresentableMetaData)
			{
				lookupElement = lookupElement.withIcon(((PsiPresentableMetaData) descriptor).getIcon());
			}
			if(xmlExtension.useXmlTagInsertHandler())
			{
				lookupElement = lookupElement.withInsertHandler(XmlTagInsertHandler.INSTANCE);
			}
			lookupElement = lookupElement.withCaseSensitivity(!(descriptor instanceof HtmlElementDescriptorImpl));
			elements.add(PrioritizedLookupElement.withPriority(lookupElement, separator > 0 ? 0 : 1));
		}
	}

	private static List<LookupElement> getRootTagsVariants(final XmlTag tag, final List<LookupElement> elements)
	{

		elements.add(LookupElementBuilder.create("?xml version=\"1.0\" encoding=\"\" ?>").withPresentableText("<?xml version=\"1.0\" encoding=\"\" ?>").withInsertHandler(new
																																												  InsertHandler<LookupElement>()
		{
			@Override
			public void handleInsert(InsertionContext context, LookupElement item)
			{
				int offset = context.getEditor().getCaretModel().getOffset();
				context.getEditor().getCaretModel().moveToOffset(offset - 4);
				AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
			}
		}));
		final FileBasedIndex fbi = FileBasedIndex.getInstance();
		Collection<String> result = new ArrayList<>();
		Processor<String> processor = Processors.cancelableCollectProcessor(result);
		fbi.processAllKeys(XmlNamespaceIndex.NAME, processor, tag.getProject());

		final GlobalSearchScope scope = new EverythingGlobalScope();
		for(final String ns : result)
		{
			if(ns.startsWith("file://"))
			{
				continue;
			}
			fbi.processValues(XmlNamespaceIndex.NAME, ns, null, new FileBasedIndex.ValueProcessor<XsdNamespaceBuilder>()
			{
				@Override
				public boolean process(final VirtualFile file, XsdNamespaceBuilder value)
				{
					List<String> tags = value.getRootTags();
					for(String s : tags)
					{
						elements.add(LookupElementBuilder.create(s).withTypeText(ns).withInsertHandler(new XmlTagInsertHandler()
						{
							@Override
							public void handleInsert(InsertionContext context, LookupElement item)
							{
								final Editor editor = context.getEditor();
								final Document document = context.getDocument();
								final int caretOffset = editor.getCaretModel().getOffset();
								final RangeMarker caretMarker = document.createRangeMarker(caretOffset, caretOffset);
								caretMarker.setGreedyToRight(true);

								XmlFile psiFile = (XmlFile) context.getFile();
								boolean incomplete = XmlUtil.getTokenOfType(tag, XmlTokenType.XML_TAG_END) == null && XmlUtil.getTokenOfType(tag, XmlTokenType.XML_EMPTY_ELEMENT_END) == null;
								XmlNamespaceHelper.getHelper(psiFile).insertNamespaceDeclaration(psiFile, editor, Collections.singleton(ns), null, null);
								editor.getCaretModel().moveToOffset(caretMarker.getEndOffset());

								XmlTag rootTag = psiFile.getRootTag();
								if(incomplete)
								{
									XmlToken token = XmlUtil.getTokenOfType(rootTag, XmlTokenType.XML_EMPTY_ELEMENT_END);
									if(token != null)
									{
										token.delete(); // remove tag end added by smart attribute adder :(
									}
									PsiDocumentManager.getInstance(context.getProject()).doPostponedOperationsAndUnblockDocument(document);
									super.handleInsert(context, item);
								}
							}
						}));
					}
					return true;
				}
			}, scope);
		}
		return elements;
	}
}
