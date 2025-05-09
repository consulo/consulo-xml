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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.document.util.TextRange;
import consulo.util.collection.ArrayUtil;
import consulo.language.psi.PsiElement;

/**
 * @author peter
*/
public class AttributeValueSelfReference extends BasicAttributeValueReference {
  public AttributeValueSelfReference(final PsiElement element) {
    super(element);
  }

  public AttributeValueSelfReference(final PsiElement element, int offset) {
    super(element, offset);
  }

  public AttributeValueSelfReference(final PsiElement element, TextRange range) {
    super(element, range);
  }

  @Nullable
  public PsiElement resolve() {
    return myElement;
  }

  @Nonnull
  public Object[] getVariants() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  public boolean isSoft() {
    return true;
  }
}
