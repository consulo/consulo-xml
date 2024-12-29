package com.intellij.xml.arrangement;

import consulo.language.codeStyle.arrangement.ArrangementEntry;
import consulo.language.codeStyle.arrangement.DefaultArrangementEntry;
import consulo.language.codeStyle.arrangement.TypeAwareArrangementEntry;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.arrangement.NameAwareArrangementEntry;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlElementArrangementEntry extends DefaultArrangementEntry implements TypeAwareArrangementEntry, NameAwareArrangementEntry {
    private final ArrangementSettingsToken myType;
    private final String myName;

    public XmlElementArrangementEntry(
        @Nullable ArrangementEntry parent,
        @Nonnull TextRange range,
        @Nonnull ArrangementSettingsToken type,
        @Nullable String name,
        boolean canBeMatched
    ) {
        super(parent, range.getStartOffset(), range.getEndOffset(), canBeMatched);
        myName = name;
        myType = type;
    }

    @Nullable
    @Override
    public String getName() {
        return myName;
    }

    @Nonnull
    @Override
    public Set<ArrangementSettingsToken> getTypes() {
        return Collections.singleton(myType);
    }
}
