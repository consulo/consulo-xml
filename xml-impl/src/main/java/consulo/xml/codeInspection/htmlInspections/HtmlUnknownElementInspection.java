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
package consulo.xml.codeInspection.htmlInspections;

import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.ast.ASTNode;
import consulo.logging.Logger;
import consulo.util.xml.serializer.JDOMExternalizableStringList;
import consulo.util.lang.StringUtil;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlChildRole;
import consulo.language.editor.inspection.ProblemsHolder;

public abstract class HtmlUnknownElementInspection extends HtmlLocalInspectionTool implements XmlEntitiesInspection
{
	public JDOMExternalizableStringList myValues;
	public boolean myCustomValuesEnabled = true;

	public HtmlUnknownElementInspection(@Nonnull String defaultValues)
	{
		myValues = reparseProperties(defaultValues);
	}

	protected static JDOMExternalizableStringList reparseProperties(@Nonnull final String properties)
	{
		final JDOMExternalizableStringList result = new JDOMExternalizableStringList();

		final StringTokenizer tokenizer = new StringTokenizer(properties, ",");
		while(tokenizer.hasMoreTokens())
		{
			result.add(tokenizer.nextToken().toLowerCase().trim());
		}

		return result;
	}

	protected static void registerProblemOnAttributeName(@Nonnull XmlAttribute attribute, String message, @Nonnull ProblemsHolder holder, LocalQuickFix... quickfixes)
	{
		final ASTNode node = attribute.getNode();
		assert node != null;
		final ASTNode nameNode = XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(node);
		if(nameNode != null)
		{
			final PsiElement nameElement = nameNode.getPsi();
			if(nameElement.getTextLength() > 0)
			{
				holder.registerProblem(nameElement, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, quickfixes);
			}
		}
	}

	protected boolean isCustomValue(@Nonnull final String value)
	{
		return myValues.contains(value.toLowerCase());
	}

	@Override
	public void addEntry(@Nonnull final String text)
	{
		final String s = text.trim().toLowerCase();
		if(!isCustomValue(s))
		{
			myValues.add(s);
		}

		if(!isCustomValuesEnabled())
		{
			myCustomValuesEnabled = true;
		}
	}

	public boolean isCustomValuesEnabled()
	{
		return myCustomValuesEnabled;
	}

	@Override
	public String getAdditionalEntries()
	{
		return StringUtil.join(myValues, ",");
	}

	public void enableCustomValues(boolean customValuesEnabled)
	{
		myCustomValuesEnabled = customValuesEnabled;
	}

	public void updateAdditionalEntries(@Nonnull final String values)
	{
		myValues = reparseProperties(values);
	}

	protected abstract String getCheckboxTitle();

	@Nonnull
	protected abstract String getPanelTitle();

	@Nonnull
	protected abstract Logger getLogger();
}
