/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.references;

import consulo.util.collection.ContainerUtil;
import consulo.xml.psi.xml.XmlAttributeValue;

import java.util.Set;
import java.util.function.Function;

class AttributeValueFunction implements Function<XmlAttributeValue, String> {
  @Override
  public String apply(XmlAttributeValue value) {
    return value.getValue();
  }

  public static String[] toStrings(Set<XmlAttributeValue> values) {
    return ContainerUtil.map2Array(values, String.class, new AttributeValueFunction());
  }
}