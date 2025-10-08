/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.xml.util.XmlTagUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author spleaner
 */
public class RenameTagBeginOrEndIntentionAction implements SyntheticIntentionAction {
    private final boolean myStart;
    private final String myTargetName;
    private final String mySourceName;

    RenameTagBeginOrEndIntentionAction(@Nonnull String targetName, @Nonnull String sourceName, boolean start) {
        myTargetName = targetName;
        mySourceName = sourceName;
        myStart = start;
    }

    @Nonnull
    @Override
    public LocalizeValue getText() {
        return myStart
            ? XmlErrorLocalize.renameStartTagNameIntention(mySourceName, myTargetName)
            : XmlErrorLocalize.renameEndTagNameIntention(mySourceName, myTargetName);
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    @RequiredWriteAction
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        int offset = editor.getCaretModel().getOffset();
        PsiElement psiElement = file.findElementAt(offset);

        if (psiElement == null || !psiElement.isValid()) {
            return;
        }
        if (!FileModificationService.getInstance().prepareFileForWrite(psiElement.getContainingFile())) {
            return;
        }

        if (psiElement instanceof PsiWhiteSpace) {
            psiElement = PsiTreeUtil.prevLeaf(psiElement);
        }
        if (psiElement instanceof XmlToken token) {
            IElementType tokenType = token.getTokenType();
            if (tokenType != XmlTokenType.XML_NAME && tokenType == XmlTokenType.XML_TAG_END) {
                psiElement = psiElement.getPrevSibling();
                if (psiElement == null) {
                    return;
                }
            }

            PsiElement target;
            String text = psiElement.getText();
            if (!myTargetName.equals(text)) {
                target = psiElement;
            }
            else {
                // we're in the other
                target = findOtherSide(psiElement, myStart);
            }

            if (target != null) {
                Document document = PsiDocumentManager.getInstance(project).getDocument(file);
                if (document != null) {
                    TextRange textRange = target.getTextRange();
                    document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), myTargetName);
                }
            }

        }
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement findOtherSide(PsiElement psiElement, boolean start) {
        PsiElement target = null;
        PsiElement parent = psiElement.getParent();
        if (parent instanceof PsiErrorElement) {
            parent = parent.getParent();
        }

        if (parent instanceof XmlTag tag) {
            if (start) {
                target = XmlTagUtil.getStartTagNameElement(tag);
            }
            else {
                target = XmlTagUtil.getEndTagNameElement(tag);
                if (target == null) {
                    PsiErrorElement errorElement = PsiTreeUtil.getChildOfType(parent, PsiErrorElement.class);
                    target = XmlWrongClosingTagNameInspection.findEndTagName(errorElement);
                }
            }
        }
        return target;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
