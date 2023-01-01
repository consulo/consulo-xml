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

import consulo.application.util.function.Processor;
import consulo.xml.util.xml.EvaluatedXmlName;
import consulo.xml.util.xml.JavaMethod;
import consulo.xml.util.xml.XmlName;
import consulo.xml.util.xml.reflect.AbstractDomChildrenDescription;
import consulo.xml.util.xml.reflect.CustomDomChildrenDescription;
import consulo.xml.util.xml.reflect.DomGenericInfo;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author peter
 */
public abstract class DomGenericInfoEx implements DomGenericInfo {

  public abstract boolean checkInitialized();

  @Nullable
  public abstract Invocation createInvocation(final JavaMethod method);

  @Nonnull
  public abstract List<AttributeChildDescriptionImpl> getAttributeChildrenDescriptions();

  @Nullable
  public final AbstractDomChildrenDescription findChildrenDescription(DomInvocationHandler handler, final String localName, String namespace,
                                                               boolean attribute,
                                                               final String qName) {
    for (final AbstractDomChildrenDescription description : getChildrenDescriptions()) {
      if (description instanceof DomChildDescriptionImpl && description instanceof AttributeChildDescriptionImpl == attribute) {
        final XmlName xmlName = ((DomChildDescriptionImpl)description).getXmlName();
        if (attribute && StringUtil.isEmpty(namespace) && xmlName.getLocalName().equals(localName)) return description;

        final EvaluatedXmlName evaluatedXmlName = handler.createEvaluatedXmlName(xmlName);
        if (DomImplUtil.isNameSuitable(evaluatedXmlName, localName, qName, namespace, handler.getFile())) {
          return description;
        }
      }
    }

    List<? extends CustomDomChildrenDescription> list = getCustomNameChildrenDescription();
    for (CustomDomChildrenDescription description : list) {
      if (attribute) {
        // todo
      }
      else if (description.getTagNameDescriptor() != null) {
        return description;
      }
    }
    return null;
  }

  public abstract boolean processAttributeChildrenDescriptions(Processor<AttributeChildDescriptionImpl> processor);
}
