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
package consulo.xml.codeInsight.template;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.context.FileTypeBasedContextType;
import consulo.language.psi.PsiFile;
import consulo.xml.ide.highlighter.HtmlFileType;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xhtml.XHTMLLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl
public class HtmlContextType extends FileTypeBasedContextType {
    public HtmlContextType() {
        super("HTML", CodeInsightLocalize.dialogEditTemplateCheckboxHtml(), HtmlFileType.INSTANCE);
    }

    @Override
    @RequiredReadAction
    public boolean isInContext(@Nonnull PsiFile file, int offset) {
        return isMyLanguage(file.getLanguage()) && !XmlContextType.isEmbeddedContent(file, offset);
    }

    static boolean isMyLanguage(Language language) {
        return language.isKindOf(HTMLLanguage.INSTANCE) || language.isKindOf(XHTMLLanguage.INSTANCE);
    }
}