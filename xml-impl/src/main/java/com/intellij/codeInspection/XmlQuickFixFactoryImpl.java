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
package com.intellij.codeInspection;

import com.intellij.codeInsight.daemon.impl.analysis.CreateNSDeclarationIntentionFix;
import com.intellij.codeInsight.daemon.impl.analysis.InsertRequiredAttributeFix;
import com.intellij.codeInspection.htmlInspections.AddAttributeValueIntentionFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
public class XmlQuickFixFactoryImpl extends XmlQuickFixFactory
{
	@Nonnull
	@Override
	public LocalQuickFixAndIntentionActionOnPsiElement insertRequiredAttributeFix(@Nonnull XmlTag tag, @Nonnull String attrName, @Nonnull String... values)
	{
		return new InsertRequiredAttributeFix(tag, attrName, values);
	}

	@Nonnull
	@Override
	public LocalQuickFix createNSDeclarationIntentionFix(@Nonnull PsiElement element, @Nonnull String namespacePrefix, @Nullable XmlToken token)
	{
		return new CreateNSDeclarationIntentionFix(element, namespacePrefix, token);
	}

	@Nonnull
	@Override
	public LocalQuickFixAndIntentionActionOnPsiElement addAttributeValueFix(@Nonnull XmlAttribute attribute)
	{
		return new AddAttributeValueIntentionFix(attribute);
	}
}
