package com.intellij.util.xml.impl;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomChildrenDescription;
import javax.annotation.Nonnull;

import java.lang.reflect.Type;

/**
 * @author peter
 */
public abstract class DomChildDescriptionImpl extends AbstractDomChildDescriptionImpl implements DomChildrenDescription {
  private final XmlName myTagName;

  protected DomChildDescriptionImpl(final XmlName tagName, @Nonnull final Type type) {
    super(type);
    myTagName = tagName;
  }

  public String getName() {
    return myTagName.getLocalName();
  }

  @Nonnull
  public String getXmlElementName() {
    return myTagName.getLocalName();
  }

  @Nonnull
  public final XmlName getXmlName() {
    return myTagName;
  }

  @Nonnull
  public String getCommonPresentableName(@Nonnull DomElement parent) {
    return getCommonPresentableName(getDomNameStrategy(parent));
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!super.equals(o)) return false;

    final DomChildDescriptionImpl that = (DomChildDescriptionImpl)o;

    if (myTagName != null ? !myTagName.equals(that.myTagName) : that.myTagName != null) return false;

    return true;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myTagName != null ? myTagName.hashCode() : 0);
    return result;
  }

  public int compareTo(final AbstractDomChildDescriptionImpl o) {
    return o instanceof DomChildDescriptionImpl ? myTagName.compareTo(((DomChildDescriptionImpl)o).myTagName) : 1;
  }
}
