// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xml.actions;

import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.actions.BaseCodeInsightAction;
import consulo.document.Document;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlEntityDecl;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.Html5SchemaProvider;
import com.intellij.xml.util.XmlUtil;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;

import javax.annotation.Nonnull;

/**
 * @author Dennis.Ushakov
 */
public class EscapeEntitiesAction extends BaseCodeInsightAction implements CodeInsightActionHandler
{
	private static String escape(XmlFile file, IntObjectMap<String> map, String text, int start)
	{
		final StringBuilder result = new StringBuilder();
		for(int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			final PsiElement element = file.findElementAt(start + i);
			if(element != null && isCharacterElement(element))
			{
				if(c == '<' || c == '>' || c == '&' || c == '"' || c == '\'' || c > 0x7f)
				{
					final String escape = map.get(c);
					if(escape != null)
					{
						result.append("&").append(escape).append(";");
						continue;
					}
				}
			}
			result.append(c);
		}
		return result.toString();
	}

	@Nonnull
	private static IntObjectMap<String> computeMap(XmlFile xmlFile)
	{
		final XmlFile file = XmlUtil.findXmlFile(xmlFile, Html5SchemaProvider.getCharsDtdLocation());
		assert file != null;

		final IntObjectMap<String> result = IntMaps.newIntObjectHashMap();
		XmlUtil.processXmlElements(file, element -> {
			if(element instanceof XmlEntityDecl)
			{
				final String value = ((XmlEntityDecl) element).getValueElement().getValue();
				final int key = Integer.parseInt(value.substring(2, value.length() - 1));
				if(!result.containsKey(key))
				{
					result.put(key, ((XmlEntityDecl) element).getName());
				}
			}
			return true;
		}, true);
		return result;
	}

	private static boolean isCharacterElement(PsiElement element)
	{
		final IElementType type = element.getNode().getElementType();
		if(type == XmlTokenType.XML_DATA_CHARACTERS)
		{
			return true;
		}
		if(type == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
		{
			if(element.getParent().getParent() instanceof XmlAttribute)
			{
				return true;
			}
		}
		if(type == XmlTokenType.XML_BAD_CHARACTER)
		{
			return true;
		}
		if(type == XmlTokenType.XML_START_TAG_START)
		{
			if(element.getNextSibling() instanceof PsiErrorElement)
			{
				return true;
			}
			if(element.getParent() instanceof PsiErrorElement)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file)
	{
		return file instanceof XmlFile;
	}

	@Nonnull
	@Override
	protected CodeInsightActionHandler getHandler()
	{
		return this;
	}

	@Override
	public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file)
	{
		int[] starts = editor.getSelectionModel().getBlockSelectionStarts();
		int[] ends = editor.getSelectionModel().getBlockSelectionEnds();
		final Document document = editor.getDocument();
		XmlFile xmlFile = (XmlFile) file;
		IntObjectMap<String> map = computeMap(xmlFile);
		for(int i = starts.length - 1; i >= 0; i--)
		{
			final int start = starts[i];
			final int end = ends[i];
			String oldText = document.getText(new TextRange(start, end));
			final String newText = escape(xmlFile, map, oldText, start);
			if(!oldText.equals(newText))
			{
				document.replaceString(start, end, newText);
			}
		}
	}

	@Override
	public boolean startInWriteAction()
	{
		return true;
	}
}
