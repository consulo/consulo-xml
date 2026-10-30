// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.html;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.html.language.HTMLLanguage;
import consulo.xml.language.XMLLanguage;

import java.util.*;

/**
 * @author Maxim.Mossienko
 */
@ExtensionImpl
public class HtmlLineMarkerProvider implements LineMarkerProvider {
    @RequiredReadAction
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(final PsiElement element) {
        if (element instanceof PsiWhiteSpace) {
            return null;
        }
        final Language language = element.getLanguage();

        if (!(language instanceof XMLLanguage)) {
            List<LineMarkerProvider> markerProviders = LineMarkerProvider.forLanguage(language);
            for (LineMarkerProvider provider : markerProviders) {
                if (!isEmbeddedLanguageProvider(provider)) {
                    continue;
                }
                LineMarkerInfo<?> info = provider.getLineMarkerInfo(element);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }

    @RequiredReadAction
    @Override
    public void collectSlowLineMarkers(final List<PsiElement> elements, final Collection<LineMarkerInfo> result) {
        Map<LineMarkerProvider, List<PsiElement>> embeddedLineMarkersWorkItems = null;

        for (PsiElement element : elements) {
            if (element instanceof PsiWhiteSpace) {
                continue;
            }
            final Language language = element.getLanguage();

            if (!(language instanceof XMLLanguage)) {
                List<LineMarkerProvider> lineMarkerProviders = LineMarkerProvider.forLanguage(language);
                for (LineMarkerProvider provider : lineMarkerProviders) {
                    if (!isEmbeddedLanguageProvider(provider)) {
                        continue;
                    }
                    if (embeddedLineMarkersWorkItems == null) {
                        embeddedLineMarkersWorkItems = new HashMap<>();
                    }
                    embeddedLineMarkersWorkItems.computeIfAbsent(provider, k -> new ArrayList<>(5)).add(element);
                }
            }
        }

        if (embeddedLineMarkersWorkItems != null) {
            for (Map.Entry<LineMarkerProvider, List<PsiElement>> entry : embeddedLineMarkersWorkItems.entrySet()) {
                entry.getKey().collectSlowLineMarkers(entry.getValue(), result);
            }
        }
    }

    /**
     * The providers of the embedded language, without those of {@link Language#ANY}: the line marker pass queries them for
     * the HTML file itself, asking them again here would duplicate their markers.
     */
    private static boolean isEmbeddedLanguageProvider(LineMarkerProvider provider) {
        return !(provider instanceof HtmlLineMarkerProvider) && provider.getLanguage() != Language.ANY;
    }

    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }
}
