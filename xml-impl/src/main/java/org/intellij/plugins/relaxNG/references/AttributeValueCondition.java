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

import javax.annotation.Nonnull;

import consulo.language.pattern.PatternCondition;
import com.intellij.psi.xml.XmlAttributeValue;
import consulo.language.util.ProcessingContext;

class AttributeValueCondition extends PatternCondition<XmlAttributeValue> {
  private final String myRef;

  public AttributeValueCondition(String ref) {
    super("AttributeValue");
    myRef = ref;
  }

  @Override
  public boolean accepts(@Nonnull XmlAttributeValue value, ProcessingContext context) {
    return myRef.equals(value.getValue());
  }
}