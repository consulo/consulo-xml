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

import consulo.annotation.component.ExtensionImpl;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.editorActions.wordSelection.PlainTextLineSelectioner;
import consulo.language.editor.action.ExtendWordSelectionHandlerBase;
import consulo.language.psi.PsiElement;

import java.util.List;

/**
 * @author yole
 */
@ExtensionImpl
public class XmlLineSelectioner extends ExtendWordSelectionHandlerBase {
  public boolean canSelect(final PsiElement e) {
    return e instanceof XmlToken && ((XmlToken)e).getTokenType() == XmlTokenType.XML_DATA_CHARACTERS;
  }

  @Override
  public List<TextRange> select(final PsiElement e, final CharSequence editorText, final int cursorOffset, final Editor editor) {
    return PlainTextLineSelectioner.selectPlainTextLine(e, editorText, cursorOffset);
  }
}