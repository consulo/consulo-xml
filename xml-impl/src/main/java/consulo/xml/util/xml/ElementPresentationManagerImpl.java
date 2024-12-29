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

package consulo.xml.util.xml;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@ServiceImpl
public class ElementPresentationManagerImpl extends ElementPresentationManager {

  @Nonnull
  public <T> Object[] createVariants(Collection<T> elements, Function<T, String> namer, int iconFlags) {
    List<Object> result = new ArrayList<Object>(elements.size());
    for (T element : elements) {
      String name = namer.apply(element);
      if (name != null) {
        Object value = createVariant(element, name, null);
        result.add(value);
      }
    }
    return result.toArray();
  }

  public Object createVariant(final Object variant, final String name, final PsiElement psiElement) {
    final LookupElementBuilder builder;
    if (psiElement != null) {
      builder = LookupElementBuilder.create(psiElement, name);
    }
    else {
      builder = LookupElementBuilder.create(name);
    }
    return builder.withIcon(ElementPresentationManager.getIcon(variant));
  }
}
