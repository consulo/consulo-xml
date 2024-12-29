package com.intellij.xml.arrangement;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlArrangementParseInfo {
    private final List<XmlElementArrangementEntry> myEntries = new ArrayList<>();

    @Nonnull
    public List<XmlElementArrangementEntry> getEntries() {
        return myEntries;
    }

    public void addEntry(@Nonnull XmlElementArrangementEntry entry) {
        myEntries.add(entry);
    }
}
