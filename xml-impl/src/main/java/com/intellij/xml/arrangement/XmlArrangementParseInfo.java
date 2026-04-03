package com.intellij.xml.arrangement;


import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlArrangementParseInfo {
    private final List<XmlElementArrangementEntry> myEntries = new ArrayList<>();

    public List<XmlElementArrangementEntry> getEntries() {
        return myEntries;
    }

    public void addEntry(XmlElementArrangementEntry entry) {
        myEntries.add(entry);
    }
}
