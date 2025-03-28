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
package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.codeEditor.Editor;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlAttribute;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Maxim.Mossienko
 */
public class RemoveAttributeIntentionFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final String myLocalName;

    public RemoveAttributeIntentionFix(final String localName, final @Nonnull XmlAttribute attribute) {
        super(attribute);
        myLocalName = localName;
    }

    @Nonnull
    public String getText() {
        return XmlErrorLocalize.removeAttributeQuickfixText(myLocalName).get();
    }

    @Nonnull
    public String getFamilyName() {
        return XmlErrorLocalize.removeAttributeQuickfixFamily().get();
    }

    @Override
    public void invoke(
        @Nonnull Project project,
        @Nonnull PsiFile file,
        @Nullable Editor editor,
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement
    ) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        PsiElement next = findNextAttribute((XmlAttribute)startElement);
        startElement.delete();

        if (next != null && editor != null) {
            editor.getCaretModel().moveToOffset(next.getTextRange().getStartOffset());
        }
    }

    @Nullable
    private static PsiElement findNextAttribute(final XmlAttribute attribute) {
        PsiElement nextSibling = attribute.getNextSibling();
        while (nextSibling != null) {
            if (nextSibling instanceof XmlAttribute) {
                return nextSibling;
            }
            nextSibling = nextSibling.getNextSibling();
        }
        return null;
    }

    public boolean startInWriteAction() {
        return true;
    }
}
