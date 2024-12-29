/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.xml.lang.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.psi.formatter.FormattingDocumentModelImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.*;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.formatter.xml.XmlBlock;
import consulo.xml.psi.formatter.xml.XmlPolicy;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class XmlFormattingModelBuilder implements FormattingModelBuilder
{
	@Nonnull
	public FormattingModel createModel(FormattingContext context)
	{
		PsiElement element = context.getPsiElement();
		CodeStyleSettings settings = context.getCodeStyleSettings();

		final ASTNode root = TreeUtil.getFileElement((TreeElement) element.getNode());
		final FormattingDocumentModel documentModel = FormattingDocumentModelImpl.createOn(element.getContainingFile());
		return new XmlFormattingModel(element.getContainingFile(), new XmlBlock(root, null, null, new XmlPolicy(settings, documentModel), null, null, false), documentModel);
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XMLLanguage.INSTANCE;
	}
}
