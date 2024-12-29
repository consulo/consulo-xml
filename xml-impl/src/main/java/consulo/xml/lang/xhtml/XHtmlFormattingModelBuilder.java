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

/*
 * @author max
 */
package consulo.xml.lang.xhtml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.codeStyle.*;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiFile;
import consulo.xml.lang.xml.XmlFormattingModel;
import consulo.xml.psi.formatter.xml.HtmlPolicy;
import consulo.xml.psi.formatter.xml.XmlBlock;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class XHtmlFormattingModelBuilder implements FormattingModelBuilder
{
	@Nonnull
	@Override
	public FormattingModel createModel(@Nonnull FormattingContext context)
	{
		final PsiFile psiFile = context.getContainingFile();
		CodeStyleSettings settings = context.getCodeStyleSettings();
		final FormattingDocumentModel documentModel = FormattingDocumentModel.create(psiFile);
		return new XmlFormattingModel(psiFile, new XmlBlock(SourceTreeToPsiMap.psiElementToTree(psiFile), null, null, new HtmlPolicy(settings, documentModel), null, null, false), documentModel);
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return XHTMLLanguage.INSTANCE;
	}
}