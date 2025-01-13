/*
 * Copyright 2013 Consulo.org
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
package consulo.xml.codeInsight.navigation;

import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.ElementPresentationManager;
import consulo.xml.util.xml.highlighting.DomElementAnnotationHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 23:22/09.10.13
 */
public class DomNavigationGutterIconBuilder<T> extends NavigationGutterIconBuilder<T> {
  public static final Function<DomElement, Collection<? extends PsiElement>> DEFAULT_DOM_CONVERTOR = new Function<DomElement, Collection<? extends PsiElement>>() {
    @Nonnull
    public Collection<? extends PsiElement> apply(final DomElement o) {
      return ContainerUtil.createMaybeSingletonList(o.getXmlElement());
    }
  };
  public static final Function<DomElement, Collection<? extends GotoRelatedItem>> DOM_GOTO_RELATED_ITEM_PROVIDER = new Function<DomElement, Collection<? extends GotoRelatedItem>>() {
    @Nonnull
    @Override
    public Collection<? extends GotoRelatedItem> apply(DomElement dom) {
      if (dom.getXmlElement() != null) {
        return Collections.singletonList(new DomGotoRelatedItem(dom));
      }
      return Collections.emptyList();
    }
  };

  public static DomNavigationGutterIconBuilder<PsiElement> create(@Nonnull final Image icon) {
    return create(icon, DEFAULT_PSI_CONVERTOR, PSI_GOTO_RELATED_ITEM_PROVIDER);
  }

  public static <T> DomNavigationGutterIconBuilder<T> create(@Nonnull final Image icon, @Nonnull Function<T, Collection<? extends PsiElement>> converter) {
    return create(icon, converter, null);
  }

  public static <T> DomNavigationGutterIconBuilder<T> create(@Nonnull final Image icon,
                                                             @Nonnull Function<T, Collection<? extends PsiElement>> converter,
                                                             final @Nullable Function<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
    return new DomNavigationGutterIconBuilder<T>(icon, converter, gotoRelatedItemProvider);
  }

  protected DomNavigationGutterIconBuilder(@Nonnull Image icon, @Nonnull Function<T, Collection<? extends PsiElement>> converter) {
    super(icon, converter);
  }

  protected DomNavigationGutterIconBuilder(@Nonnull Image icon,
                                           @Nonnull Function<T, Collection<? extends PsiElement>> converter,
                                           @Nullable Function<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
    super(icon, converter, gotoRelatedItemProvider);
  }

  @Nullable
  public Annotation install(@Nonnull DomElementAnnotationHolder holder, @Nullable DomElement element) {
    if (!myLazy && myTargets.get().isEmpty() || element == null) {
      return null;
    }
    return doInstall(holder.createAnnotation(element, HighlightSeverity.INFORMATION, null), element.getManager().getProject());
  }

  @Override
  protected Function<T, String> createDefaultNamer() {
    return ElementPresentationManager.namer();
  }
}
