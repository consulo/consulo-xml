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
package com.intellij.codeInsight.editorActions;

import com.intellij.psi.impl.source.xml.behavior.EncodeEachSymbolPolicy;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlText;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RawText;
import consulo.document.Document;
import consulo.language.ast.ASTNode;
import consulo.language.editor.action.CopyPastePreProcessor;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;

import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class XmlCopyPastePreProcessor implements CopyPastePreProcessor {

  private static final EncodeEachSymbolPolicy ENCODE_EACH_SYMBOL_POLICY = new EncodeEachSymbolPolicy();

  @Nullable
  public String preprocessOnCopy(PsiFile file, int[] startOffsets, int[] endOffsets, String text) {
    return null;
  }

  public String preprocessOnPaste(Project project, PsiFile file, Editor editor, String text, RawText rawText) {
    final Document document = editor.getDocument();
    PsiDocumentManager.getInstance(project).commitDocument(document);
    int caretOffset = editor.getCaretModel().getOffset();
    PsiElement element = PsiUtilCore.getElementAtOffset(file, caretOffset);

    ASTNode node = element.getNode();
    if (node != null) {
      boolean hasMarkup = text.indexOf('>') >= 0 || text.indexOf('<') >= 0;
      if (element.getTextOffset() == caretOffset &&
          node.getElementType() == XmlElementType.XML_END_TAG_START &&
          node.getTreePrev().getElementType() == XmlElementType.XML_TAG_END) {

         return hasMarkup ? text : encode(text, element);
      } else {
        XmlElement parent = PsiTreeUtil.getParentOfType(element, XmlText.class, XmlAttributeValue.class);
        if (parent != null) {
          if (parent instanceof XmlText && hasMarkup) {
            return text;
          }

          if (TreeUtil.findParent(node, XmlElementType.XML_CDATA) == null &&
              TreeUtil.findParent(node, XmlElementType.XML_COMMENT) == null) {
            return encode(text, element);
          }
        }
      }
    }
    return text;
  }

  private static String encode(String text, PsiElement element) {
    ASTNode astNode = ENCODE_EACH_SYMBOL_POLICY.encodeXmlTextContents(text, element);
    return astNode.getTreeParent().getText();
  }
}
