/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.xml.codeInsight.editorActions.moveLeftRight;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.moveLeftRight.MoveElementLeftRightHandler;
import consulo.language.psi.PsiElement;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class XmlMoveLeftRightHandler implements MoveElementLeftRightHandler {
    @RequiredReadAction
    @Nonnull
    @Override
    public PsiElement[] getMovableSubElements(@Nonnull PsiElement element) {
        if (element instanceof XmlTag) {
            return ((XmlTag)element).getAttributes();
        }
        return PsiElement.EMPTY_ARRAY;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
