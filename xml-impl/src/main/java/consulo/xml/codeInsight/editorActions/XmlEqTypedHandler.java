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
package consulo.xml.codeInsight.editorActions;

import javax.annotation.Nonnull;

import consulo.xml.lang.xml.XMLLanguage;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.language.editor.action.TypedHandlerDelegate;

public class XmlEqTypedHandler extends TypedHandlerDelegate {
  private boolean needToInsertQuotes = false;

  @Override
  public Result beforeCharTyped(char c,
                                Project project,
                                Editor editor,
                                PsiFile file,
                                FileType fileType) {
    boolean inXml = file.getLanguage() instanceof XMLLanguage || file.getViewProvider().getBaseLanguage() instanceof XMLLanguage;
    if (c == '=' && inXml) {
      int offset = editor.getCaretModel().getOffset();
      PsiElement at = file.findElementAt(offset - 1);
      PsiElement atParent = at != null ? at.getParent() : null;
      needToInsertQuotes = atParent instanceof XmlAttribute && ((XmlAttribute)atParent).getValueElement() == null;
    }

    return super.beforeCharTyped(c, project, editor, file, fileType);
  }

  @Override
  public Result charTyped(char c, Project project, Editor editor, @Nonnull PsiFile file) {
    if (needToInsertQuotes) {
      int offset = editor.getCaretModel().getOffset();
      editor.getDocument().insertString(offset, "\"\"");
      editor.getCaretModel().moveToOffset(offset + 1);
    }
    needToInsertQuotes = false;
    return super.charTyped(c, project, editor, file);
  }
}