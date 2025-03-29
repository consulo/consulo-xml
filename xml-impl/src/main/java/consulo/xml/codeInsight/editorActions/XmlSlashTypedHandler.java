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
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.psi.OuterLanguageElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.*;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "xmlSlash")
public class XmlSlashTypedHandler extends TypedHandlerDelegate {
    @Nonnull
    @Override
    @RequiredReadAction
    public Result beforeCharTyped(
        char c,
        @Nonnull Project project,
        @Nonnull Editor editor,
        PsiFile editedFile,
        @Nonnull FileType fileType
    ) {
        if ((editedFile.getLanguage() instanceof XMLLanguage || editedFile.getViewProvider().getBaseLanguage() instanceof XMLLanguage)
            && c == '/') {
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            int offset = editor.getCaretModel().getOffset();
            FileViewProvider provider = file.getViewProvider();
            PsiElement element = provider.findElementAt(offset, XMLLanguage.class);

            if (element instanceof XmlToken token) {
                IElementType tokenType = token.getTokenType();

                if (tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END && offset == element.getTextOffset()) {
                    editor.getCaretModel().moveToOffset(offset + 1);
                    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                    return Result.STOP;
                }
                else if (tokenType == XmlTokenType.XML_TAG_END && offset == element.getTextOffset()) {
                    ASTNode parentNode = element.getParent().getNode();
                    ASTNode child = XmlChildRole.CLOSING_TAG_START_FINDER.findChild(parentNode);

                    if (child != null && offset + 1 == child.getTextRange().getStartOffset()) {
                        editor.getDocument().replaceString(offset + 1, parentNode.getTextRange().getEndOffset(), "");
                    }
                }
            }
        }
        return Result.CONTINUE;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Result charTyped(char c, @Nonnull Project project, @Nonnull Editor editor, PsiFile editedFile) {
        if ((editedFile.getLanguage() instanceof XMLLanguage
            || editedFile.getViewProvider().getBaseLanguage() instanceof XMLLanguage) && c == '/') {
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            FileViewProvider provider = file.getViewProvider();
            int offset = editor.getCaretModel().getOffset();
            PsiElement element = provider.findElementAt(offset - 1, XMLLanguage.class);
            if (element == null) {
                return Result.CONTINUE;
            }
            if (!(element.getLanguage() instanceof XMLLanguage)) {
                return Result.CONTINUE;
            }

            ASTNode prevLeaf = element.getNode();
            String prevLeafText = prevLeaf != null ? prevLeaf.getText() : null;
            if (prevLeaf != null && !"/".equals(prevLeafText)) {
                if (!"/".equals(prevLeafText.trim())) {
                    return Result.CONTINUE;
                }
            }
            while ((prevLeaf = TreeUtil.prevLeaf(prevLeaf)) != null && prevLeaf.getElementType() == XmlTokenType.XML_WHITE_SPACE) ;
            if (prevLeaf instanceof OuterLanguageElement) {
                element = file.getViewProvider().findElementAt(offset - 1, file.getLanguage());
                prevLeaf = element.getNode();
                while ((prevLeaf = TreeUtil.prevLeaf(prevLeaf)) != null && prevLeaf.getElementType() == XmlTokenType.XML_WHITE_SPACE) ;
            }
            if (prevLeaf == null) {
                return Result.CONTINUE;
            }

            XmlTag tag = PsiTreeUtil.getParentOfType(prevLeaf.getPsi(), XmlTag.class);
            if (tag == null) { // prevLeaf maybe in one tree and element in another
                PsiElement element2 = provider.findElementAt(prevLeaf.getStartOffset(), XMLLanguage.class);
                tag = PsiTreeUtil.getParentOfType(element2, XmlTag.class);
                if (tag == null) {
                    return Result.CONTINUE;
                }
            }

            XmlToken startToken = XmlUtil.getTokenOfType(tag, XmlTokenType.XML_START_TAG_START);
            if (startToken == null || !startToken.getText().equals("<")) {
                return Result.CONTINUE;
            }
            if (XmlUtil.getTokenOfType(tag, XmlTokenType.XML_TAG_END) != null) {
                return Result.CONTINUE;
            }
            if (XmlUtil.getTokenOfType(tag, XmlTokenType.XML_EMPTY_ELEMENT_END) != null) {
                return Result.CONTINUE;
            }
            if (PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class) != null) {
                return Result.CONTINUE;
            }

            EditorModificationUtil.insertStringAtCaret(editor, ">");
            return Result.STOP;
        }
        return Result.CONTINUE;
    }
}