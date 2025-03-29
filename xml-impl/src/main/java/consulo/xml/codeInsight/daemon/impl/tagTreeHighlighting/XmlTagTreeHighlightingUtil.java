/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.xml.application.options.editor.XmlEditorOptions;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
class XmlTagTreeHighlightingUtil {
    private XmlTagTreeHighlightingUtil() {
    }

    static boolean containsTagsWithSameName(PsiElement[] elements) {
        Set<String> names = new HashSet<>();

        for (PsiElement element : elements) {
            if (element instanceof XmlTag tag) {
                String name = tag.getName();
                if (!names.add(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean isTagTreeHighlightingActive(PsiFile file) {
        if (Application.get().isUnitTestMode()) {
            return false;
        }

        return hasXmlViewProvider(file) && XmlEditorOptions.getInstance().isTagTreeHighlightingEnabled();
    }

    private static boolean hasXmlViewProvider(@Nonnull PsiFile file) {
        for (PsiFile f : file.getViewProvider().getAllFiles()) {
            if (f instanceof XmlFile) {
                return true;
            }
        }
        return false;
    }

    static RGBColor makeTransparent(@Nonnull ColorValue c, @Nonnull ColorValue bc, double transparency) {
        RGBColor color = c.toRGB();
        RGBColor backgroundColor = bc.toRGB();
        int r = makeTransparent(transparency, color.getRed(), backgroundColor.getRed());
        int g = makeTransparent(transparency, color.getGreen(), backgroundColor.getGreen());
        int b = makeTransparent(transparency, color.getBlue(), backgroundColor.getBlue());

        return new RGBColor(r, g, b);
    }

    private static int makeTransparent(double transparency, int channel, int backgroundChannel) {
        int result = (int)(backgroundChannel * (1 - transparency) + channel * transparency);
        if (result < 0) {
            return 0;
        }
        if (result > 255) {
            return 255;
        }
        return result;
    }

    static ColorValue[] getBaseColors() {
        EditorColorKey[] colorKeys = XmlTagTreeHighlightingColors.getColorKeys();
        ColorValue[] colors = new ColorValue[colorKeys.length];

        EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();

        for (int i = 0; i < colors.length; i++) {
            colors[i] = colorsScheme.getColor(colorKeys[i]);
        }

        return colors;
    }
}
