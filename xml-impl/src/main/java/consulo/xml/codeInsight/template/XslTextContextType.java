/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.codeInsight.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.context.BaseTemplateContextType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.io.FileUtil;
import consulo.xml.ide.highlighter.XmlFileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class XslTextContextType extends BaseTemplateContextType {
    public XslTextContextType() {
        super("XSL_TEXT", CodeInsightLocalize.dialogEditTemplateCheckboxXslText(), XmlContextType.class);
    }

    @Override
    public boolean isInContext(@Nonnull PsiFile file, int offset) {
        if (isXslOrXsltFile(file)) {
            PsiElement element = file.findElementAt(offset);
            return element == null || HtmlTextContextType.isInContext(element);
        }
        return false;
    }

    public static boolean isXslOrXsltFile(@Nullable PsiFile file) {
        return file != null && file.getFileType() == XmlFileType.INSTANCE
            && (FileUtil.extensionEquals(file.getName(), "xsl") || FileUtil.extensionEquals(file.getName(), "xslt"));
    }
}
