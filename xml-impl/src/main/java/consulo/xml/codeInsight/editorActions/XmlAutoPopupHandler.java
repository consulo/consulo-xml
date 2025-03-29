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
package consulo.xml.codeInsight.editorActions;

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.project.Project;
import consulo.util.lang.ref.SimpleReference;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "xmlAuto", order = "after xmlGt")
public class XmlAutoPopupHandler extends TypedHandlerDelegate {
    @Nonnull
    @Override
    @RequiredReadAction
    public Result checkAutoPopup(char charTyped, @Nonnull Project project, @Nonnull Editor editor, PsiFile file) {
        boolean isXmlLikeFile = file.getLanguage() instanceof XMLLanguage
            || file.getViewProvider().getBaseLanguage() instanceof XMLLanguage;
        boolean spaceInTag = isXmlLikeFile && charTyped == ' ';

        if (spaceInTag) {
            spaceInTag = false;
            PsiElement at = file.findElementAt(editor.getCaretModel().getOffset());

            if (at != null) {
                PsiElement parent = at.getParent();
                if (parent instanceof XmlTag) {
                    spaceInTag = true;
                }
            }
        }

        if ((charTyped == '<' || charTyped == '{' || charTyped == '/' || spaceInTag) && isXmlLikeFile) {
            autoPopupXmlLookup(project, editor);
            return Result.STOP;
        }
        return Result.CONTINUE;
    }

    public static void autoPopupXmlLookup(Project project, Editor editor) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(
            editor,
            file -> {
                int offset = editor.getCaretModel().getOffset();

                PsiElement lastElement = InjectedLanguageManager.getInstance(project).findElementAtNoCommit(file, offset - 1);
                if (lastElement instanceof PsiFile) { //the very end of an injected file
                    lastElement = file.findElementAt(offset - 1);
                }
                if (lastElement == null || !lastElement.isValid()) {
                    return false;
                }

                if (doCompleteIfNeeded(offset, file, lastElement)) {
                    return true;
                }

                FileViewProvider fileViewProvider = file.getViewProvider();
                Language templateDataLanguage;

                PsiElement parent = lastElement.getParent();
                if (fileViewProvider instanceof TemplateLanguageFileViewProvider templateLanguageFileViewProvider
                    && (templateDataLanguage = templateLanguageFileViewProvider.getTemplateDataLanguage()) != parent.getLanguage()) {
                    lastElement = fileViewProvider.findElementAt(offset - 1, templateDataLanguage);
                    return !(lastElement == null || !lastElement.isValid()) && doCompleteIfNeeded(offset, file, lastElement);
                }
                return false;
            }
        );
    }

    @RequiredReadAction
    private static boolean doCompleteIfNeeded(int offset, PsiFile file, PsiElement lastElement) {
        SimpleReference<Boolean> isRelevantLanguage = new SimpleReference<>();
        SimpleReference<Boolean> isAnt = new SimpleReference<>();
        String text = lastElement.getText();
        int len = offset - lastElement.getTextRange().getStartOffset();
        if (len < text.length()) {
            text = text.substring(0, len);
        }
        return text.equals("<") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt)
            || text.equals(" ") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt)
            || text.endsWith("${") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt) && isAnt.get()
            || text.endsWith("@{") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt) && isAnt.get()
            || text.endsWith("</") && isLanguageRelevant(lastElement, file, isRelevantLanguage, isAnt);
    }

    @RequiredReadAction
    private static boolean isLanguageRelevant(
        PsiElement element,
        PsiFile file,
        SimpleReference<Boolean> isRelevantLanguage,
        SimpleReference<Boolean> isAnt
    ) {
        Boolean isAntFile = isAnt.get();
        if (isAntFile == null) {
            isAntFile = XmlUtil.isAntFile(file);
            isAnt.set(isAntFile);
        }
        Boolean result = isRelevantLanguage.get();
        if (result == null) {
            Language language = element.getLanguage();
            PsiElement parent = element.getParent();
            if (element instanceof PsiWhiteSpace && parent != null) {
                language = parent.getLanguage();
            }
            result = language instanceof XMLLanguage || isAntFile;
            isRelevantLanguage.set(result);
        }
        return result;
    }
}