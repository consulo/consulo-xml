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

package org.intellij.plugins.relaxNG.model.annotation;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.psi.PsiElement;
import consulo.navigation.Navigatable;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import org.intellij.plugins.relaxNG.model.CommonElement;
import org.intellij.plugins.relaxNG.model.Define;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Set;

class OverridingDefineRenderer extends GutterIconRenderer implements DumbAware {

  private final Set<Define> mySet;
  private final String myMessage;

  public OverridingDefineRenderer(String message, Set<Define> set) {
    mySet = set;
    myMessage = message;
  }

  @Override
  @Nonnull
  public Image getIcon() {
    return AllIcons.Gutter.OverridingMethod;
  }

  @Override
  public boolean isNavigateAction() {
    return true;
  }

  @Override
  @Nullable
  public AnAction getClickAction() {
    return new MyClickAction();
  }

  @Override
  @Nullable
  public String getTooltipText() {
    return myMessage;
  }

  private class MyClickAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
      doClickAction(e, mySet, "Go to overridden define");
    }
  }

  static void doClickAction(AnActionEvent e, Collection<Define> set, String title) {
    if (set.size() == 1) {
      final Navigatable n = (Navigatable)set.iterator().next().getPsiElement();
      OpenSourceUtil.navigate(true, n);
    } else {
      final Define[] array = set.toArray(new Define[set.size()]);
      PopupNavigationUtil.getPsiElementPopup(ContainerUtil.map(array, CommonElement::getPsiElement, PsiElement.EMPTY_ARRAY), title).show(new RelativePoint((MouseEvent)e.getInputEvent()));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OverridingDefineRenderer that = (OverridingDefineRenderer)o;

    if (myMessage != null ? !myMessage.equals(that.myMessage) : that.myMessage != null) return false;
    if (mySet != null ? !mySet.equals(that.mySet) : that.mySet != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mySet != null ? mySet.hashCode() : 0;
    result = 31 * result + (myMessage != null ? myMessage.hashCode() : 0);
    return result;
  }
}
