// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.html;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xml.XMLLanguage;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author Maxim.Mossienko
 */
@ExtensionImpl
public class HtmlLineMarkerProvider implements LineMarkerProvider {
    @RequiredReadAction
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(final @Nonnull PsiElement element) {
        if (element instanceof PsiWhiteSpace) {
            return null;
        }
        final Language language = element.getLanguage();

        if (!(language instanceof XMLLanguage)) {
            List<LineMarkerProvider> markerProviders = LineMarkerProvider.forLanguage(language);
            for (LineMarkerProvider provider : markerProviders) {
                if (provider instanceof HtmlLineMarkerProvider) {
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
    public void collectSlowLineMarkers(final @Nonnull List<PsiElement> elements, final @Nonnull Collection<LineMarkerInfo> result) {
        Map<LineMarkerProvider, List<PsiElement>> embeddedLineMarkersWorkItems = null;

        for (PsiElement element : elements) {
            if (element instanceof PsiWhiteSpace) {
                continue;
            }
            final Language language = element.getLanguage();

            if (!(language instanceof XMLLanguage)) {
                List<LineMarkerProvider> lineMarkerProviders = LineMarkerProvider.forLanguage(language);
                for (LineMarkerProvider provider : lineMarkerProviders) {
                    if (provider instanceof HtmlLineMarkerProvider) {
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

    @Nonnull
    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }
}
