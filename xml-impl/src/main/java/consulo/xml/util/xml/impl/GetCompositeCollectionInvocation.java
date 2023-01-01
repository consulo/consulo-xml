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

import consulo.xml.psi.xml.XmlTag;
import consulo.util.collection.ContainerUtil;
import consulo.xml.util.xml.DomElement;

import java.util.*;

/**
 * @author peter
*/
class GetCompositeCollectionInvocation implements Invocation {
  private final Set<CollectionChildDescriptionImpl> myQnames;

  public GetCompositeCollectionInvocation(final Set<CollectionChildDescriptionImpl> qnames) {
    myQnames = qnames;
  }

  public Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable {
    Map<XmlTag,DomElement> map = new HashMap<XmlTag, DomElement>();
    for (final CollectionChildDescriptionImpl qname : myQnames) {
      for (DomElement element : handler.getCollectionChildren(qname, qname.getTagsGetter())) {
        map.put(element.getXmlTag(), element);
      }
    }
    final XmlTag tag = handler.getXmlTag();
    if (tag == null) return Collections.emptyList();

    final List<DomElement> list = new ArrayList<DomElement>();
    for (final XmlTag subTag : tag.getSubTags()) {
      ContainerUtil.addIfNotNull(list, map.get(subTag));
    }
    return list;
  }
}
