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
package consulo.xml.codeInsight.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.moveUpDown.MethodNavigationOffsetProvider;
import consulo.language.editor.moveUpDown.MethodUpDownUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;

import java.util.ArrayList;

/**
 * @author yole
 */
@ExtensionImpl(id = "xml")
public class XmlMethodNavigationOffsetProvider implements MethodNavigationOffsetProvider {
    @Override
    @RequiredReadAction
    public int[] getMethodNavigationOffsets(PsiFile file, int caretOffset) {
        if (file instanceof XmlFile) {
            PsiElement element = file;
            PsiElement elementAt = file.findElementAt(caretOffset);
            elementAt = PsiTreeUtil.getParentOfType(elementAt, XmlTag.class);
            if (elementAt != null) {
                element = elementAt;
            }

            ArrayList<PsiElement> array = new ArrayList<>();
            addNavigationElements(array, element);
            return MethodUpDownUtil.offsetsFromElements(array);
        }
        return null;
    }

    @RequiredReadAction
    private static void addNavigationElements(ArrayList<PsiElement> array, PsiElement element) {
        PsiElement parent = element instanceof XmlFile ? element : element.getParent();

        if (parent != null) {
            PsiElement[] children = parent.getChildren();
            for (PsiElement child : children) {
                if (child instanceof XmlTag) {
                    array.add(child);
                }
            }
        }
        PsiElement parentElement = element.getParent();
        if (parentElement != null) {
            addNavigationElements(array, parentElement);
        }
    }
}
