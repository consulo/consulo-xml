// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xml.highlighter;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;
import consulo.util.collection.MultiMap;
import consulo.xml.lang.xml.XMLLanguage;
import jakarta.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface EmbeddedTokenHighlighter {
    /**
     * @param language can be XMLLanguage, HTMLLanguage, XHTMLLanguage, DTDLanguage
     * @return a map of text attributes to be used for highlighting specific non-XML token types that can occur inside XML/HTML
     */
    @Nonnull
    MultiMap<IElementType, TextAttributesKey> getEmbeddedTokenAttributes(@Nonnull XMLLanguage language);
}
