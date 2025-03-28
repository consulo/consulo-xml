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
package consulo.xml.util.xml.impl;

import consulo.xml.util.xml.XmlName;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author peter
 */
public class ChildrenDescriptionsHolder<T extends DomChildDescriptionImpl> {
  private final Map<XmlName, T> myMap = new HashMap<XmlName, T>();
  private final ChildrenDescriptionsHolder<T> myDelegate;
  private volatile List<T> myCached = null;

  public ChildrenDescriptionsHolder(@Nullable final ChildrenDescriptionsHolder<T> delegate) {
    myDelegate = delegate;
  }

  public ChildrenDescriptionsHolder() {
    this(null);
  }

  final T addDescription(@Nonnull T t) {
    myMap.put(t.getXmlName(), t);
    myCached = null;
    return t;
  }

  final void addDescriptions(@Nonnull Collection<T> collection) {
    for (final T t : collection) {
      addDescription(t);
    }
  }

  @Nullable
  final T getDescription(final XmlName name) {
    final T t = myMap.get(name);
    if (t != null) return t;
    return myDelegate != null ? myDelegate.getDescription(name) : null;
  }

  @Nullable
  final T getDescription(@Nonnull final String localName, String namespaceKey) {
    return getDescription(new XmlName(localName, namespaceKey));
  }

  @Nullable
  final T findDescription(@Nonnull final String localName) {
    for (final XmlName xmlName : myMap.keySet()) {
      if (xmlName.getLocalName().equals(localName)) return myMap.get(xmlName);
    }
    return myDelegate != null ? myDelegate.findDescription(localName) : null;
  }

  @Nonnull
  final List<T> getDescriptions() {
    final ArrayList<T> result = new ArrayList<T>();
    dumpDescriptions(result);
    return result;
  }

  private List<T> getSortedDescriptions() {
    List<T> cached = myCached;
    if (cached != null) {
      return cached;
    }

    cached = new ArrayList<T>(myMap.values());
    Collections.sort(cached);
    myCached = cached;
    return cached;
  }


  final void dumpDescriptions(Collection<? super T> to) {
    to.addAll(getSortedDescriptions());
    if (myDelegate != null) {
      myDelegate.dumpDescriptions(to);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ChildrenDescriptionsHolder)) return false;

    ChildrenDescriptionsHolder holder = (ChildrenDescriptionsHolder)o;

    if (myDelegate != null ? !myDelegate.equals(holder.myDelegate) : holder.myDelegate != null) return false;
    if (!myMap.equals(holder.myMap)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myMap.hashCode();
    result = 31 * result + (myDelegate != null ? myDelegate.hashCode() : 0);
    return result;
  }
}
