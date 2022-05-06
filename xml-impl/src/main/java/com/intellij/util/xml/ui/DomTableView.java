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
package com.intellij.util.xml.ui;

import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.util.collection.SmartList;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author peter
 */
public class DomTableView extends AbstractTableView<DomElement> {
  private final List<TypeSafeDataProvider> myCustomDataProviders = new SmartList<TypeSafeDataProvider>();

  public DomTableView(final Project project) {
    super(project);
  }

  public DomTableView(final Project project, final String emptyPaneText, final String helpID) {
    super(project, emptyPaneText, helpID);
  }

  public void addCustomDataProvider(TypeSafeDataProvider provider) {
    myCustomDataProviders.add(provider);
  }

  public void calcData(final Key<?> key, final DataSink sink) {
    super.calcData(key, sink);
    for (final TypeSafeDataProvider customDataProvider : myCustomDataProviders) {
      customDataProvider.calcData(key, sink);
    }
  }

  @Deprecated
  protected final void installPopup(final DefaultActionGroup group) {
    installPopup(ActionPlaces.J2EE_ATTRIBUTES_VIEW_POPUP, group);
  }

  protected void wrapValueSetting(@Nonnull final DomElement domElement, final Runnable valueSetter) {
    if (domElement.isValid()) {
      new WriteCommandAction(getProject(), DomUtil.getFile(domElement)) {
        protected void run(final Result result) throws Throwable {
          valueSetter.run();
        }
      }.execute();
      fireChanged();
    }
  }

}
