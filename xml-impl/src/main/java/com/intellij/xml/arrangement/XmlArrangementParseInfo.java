package com.intellij.xml.arrangement;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlArrangementParseInfo {

  private final List<XmlElementArrangementEntry> myEntries = new ArrayList<XmlElementArrangementEntry>();

  @Nonnull
  public List<XmlElementArrangementEntry> getEntries() {
    return myEntries;
  }

  public void addEntry(@Nonnull XmlElementArrangementEntry entry) {
    myEntries.add(entry);
  }
}
