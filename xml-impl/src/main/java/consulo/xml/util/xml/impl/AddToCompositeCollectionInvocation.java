/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.xml.util.xml.EvaluatedXmlName;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
*/
class AddToCompositeCollectionInvocation implements Invocation {
  private final CollectionChildDescriptionImpl myMainDescription;
  private final Set<CollectionChildDescriptionImpl> myQnames;
  private final Type myType;

  public AddToCompositeCollectionInvocation(final CollectionChildDescriptionImpl tagName, final Set<CollectionChildDescriptionImpl> qnames, final Type type) {
    myMainDescription = tagName;
    myQnames = qnames;
    myType = type;
  }

  public Object invoke(final DomInvocationHandler<?, ?> handler, final Object[] args) throws Throwable {
    Set<XmlTag> set = new HashSet<>();
    for (final CollectionChildDescriptionImpl qname : myQnames) {
      set.addAll(qname.getTagsGetter().apply(handler));
    }

    final XmlTag tag = handler.ensureTagExists();
    int index = args != null && args.length == 1 ? (Integer)args[0] : Integer.MAX_VALUE;

    XmlTag lastTag = null;
    int i = 0;
    final XmlTag[] tags = tag.getSubTags();
    for (final XmlTag subTag : tags) {
      if (i == index) break;
      if (set.contains(subTag)) {
        lastTag = subTag;
        i++;
      }
    }
    final DomManagerImpl manager = handler.getManager();
    final boolean b = manager.setChanging(true);
    try {
      final EvaluatedXmlName evaluatedXmlName = handler.createEvaluatedXmlName(myMainDescription.getXmlName());
      final XmlTag emptyTag = handler.createChildTag(evaluatedXmlName);
      final XmlTag newTag;
      if (lastTag == null) {
        if (tags.length == 0) {
          newTag = (XmlTag)tag.add(emptyTag);
        }
        else {
          newTag = (XmlTag)tag.addBefore(emptyTag, tags[0]);
        }
      }
      else {
        newTag = (XmlTag)tag.addAfter(emptyTag, lastTag);
      }

      return new CollectionElementInvocationHandler(myType, newTag, myMainDescription, handler, null).getProxy();
    }
    finally {
      manager.setChanging(b);
    }
  }


}
