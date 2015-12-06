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

import org.consulo.lombok.annotations.ApplicationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;

@ApplicationService
public abstract class XmlQuickFixFactory
{
	@NotNull
	public abstract LocalQuickFixAndIntentionActionOnPsiElement insertRequiredAttributeFix(@NotNull XmlTag tag, @NotNull String attrName, @NotNull String... values);

	@NotNull
	public abstract LocalQuickFix createNSDeclarationIntentionFix(@NotNull final PsiElement element, @NotNull String namespacePrefix, @Nullable final XmlToken token);

	@NotNull
	public abstract LocalQuickFixAndIntentionActionOnPsiElement addAttributeValueFix(@NotNull XmlAttribute attribute);
}