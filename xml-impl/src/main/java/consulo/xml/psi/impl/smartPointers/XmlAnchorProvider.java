/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.xml.psi.impl.smartPointers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import com.intellij.xml.util.XmlTagUtil;
import consulo.language.psi.SmartPointerAnchorProvider;

/**
 * @author Dennis.Ushakov
 */
public class XmlAnchorProvider implements SmartPointerAnchorProvider
{
	@Nullable
	@Override
	public PsiElement getAnchor(@Nonnull PsiElement element)
	{
		if(element instanceof XmlTag)
		{
			return XmlTagUtil.getStartTagNameElement((XmlTag) element);
		}
		return null;
	}

	@Nullable
	@Override
	public PsiElement restoreElement(@Nonnull PsiElement anchor)
	{
		if(anchor instanceof XmlToken)
		{
			XmlToken token = (XmlToken) anchor;
			return token.getTokenType() == XmlTokenType.XML_NAME ? token.getParent() : null;
		}
		return null;
	}
}
