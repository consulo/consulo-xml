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
package consulo.xml.util.xml;

import jakarta.annotation.Nonnull;

import consulo.xml.util.xml.highlighting.DomElementsInspection;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiElement;

/**
 * Can be implemented by {@link Converter} instance, or used with
 * {@link @Referencing} annotation.
 *
 * @author peter
 */
public interface CustomReferenceConverter<T> {

  /**
   * Will be called on creating {@link PsiReference}s for {@link GenericDomValue}
   * Returned {@link PsiReference}s should be soft ({@link PsiReference#isSoft()} should return <code>true</code>).
   * To highlight unresolved references, create a {@link DomElementsInspection} and register it.
   *
   * @param value GenericDomValue in question
   * @param element corresponding PSI element
   * @param context {@link ConvertContext}
   * @return custom {@link PsiReference}s for the value
   */
  @Nonnull
  PsiReference[] createReferences(GenericDomValue<T> value, PsiElement element, ConvertContext context);
}
