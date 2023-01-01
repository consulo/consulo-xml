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

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.language.psi.PsiElement;
import org.intellij.plugins.relaxNG.model.Define;
import org.intellij.plugins.relaxNG.model.Grammar;
import org.intellij.plugins.relaxNG.model.resolve.GrammarFactory;
import org.intellij.plugins.relaxNG.model.resolve.RelaxIncludeIndex;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.xml.psi.xml.XmlFile;
import consulo.util.collection.SmartList;
import consulo.ui.image.Image;

class OverriddenDefineRenderer extends GutterIconRenderer implements DumbAware {

  private final Define myDefine;

  public OverriddenDefineRenderer(@Nonnull Define define) {
    myDefine = define;
  }

  @Override
  @Nonnull
  public Image getIcon() {
    return AllIcons.Gutter.OverridenMethod;
  }

  @Override
  @Nullable
  public AnAction getClickAction() {
    return new AnAction() {
      @Override
      public void actionPerformed(AnActionEvent e) {
        final PsiElement element = myDefine.getPsiElement();
        if (element == null || !element.isValid()) return;

        final PsiElementProcessor.CollectElements<XmlFile> collector = new PsiElementProcessor.CollectElements<>();
        final XmlFile localFile = (XmlFile)element.getContainingFile();
        RelaxIncludeIndex.processBackwardDependencies(localFile, collector);
        final Collection<XmlFile> files = collector.getCollection();

        final List<Define> result = new SmartList<>();
        final OverriddenDefineSearcher searcher = new OverriddenDefineSearcher(myDefine, localFile, result);
        for (XmlFile file : files) {
          final Grammar grammar = GrammarFactory.getGrammar(file);
          if (grammar == null) continue;
          grammar.acceptChildren(searcher);
        }

        if (result.size() > 0) {
          OverridingDefineRenderer.doClickAction(e, result, "Go to overriding define(s)");
        }
      }
    };
  }

  @Override
  public boolean isNavigateAction() {
    return true;
  }

  @Override
  @Nullable
  public String getTooltipText() {
    return "Is overridden";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OverriddenDefineRenderer that = (OverriddenDefineRenderer)o;

    if (!myDefine.equals(that.myDefine)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myDefine.hashCode();
  }
}
