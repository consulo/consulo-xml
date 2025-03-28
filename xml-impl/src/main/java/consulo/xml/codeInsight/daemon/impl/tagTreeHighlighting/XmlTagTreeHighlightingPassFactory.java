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
package consulo.xml.codeInsight.daemon.impl.tagTreeHighlighting;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassFactory;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class XmlTagTreeHighlightingPassFactory implements TextEditorHighlightingPassFactory {
    @Override
    public void register(@Nonnull Registrar registrar) {
        registrar.registerTextEditorHighlightingPass(this, new int[]{Pass.UPDATE_ALL}, null, false, -1);
    }

    @Override
    public TextEditorHighlightingPass createHighlightingPass(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
        if (editor.isOneLineMode()) {
            return null;
        }

        if (!XmlTagTreeHighlightingUtil.isTagTreeHighlightingActive(file)) {
            return null;
        }
        if (!(editor instanceof EditorEx)) {
            return null;
        }

        return new XmlTagTreeHighlightingPass(file, (EditorEx)editor);
    }
}
