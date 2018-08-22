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
package com.intellij.javaee;

import javax.inject.Inject;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.impl.PsiManagerEx;

/**
 * @author yole
 */
public class PsiExternalResourceNotifier {
  private final PsiManagerEx myPsiManager;
  private final ExternalResourceManagerEx myExternalResourceManager;
  private final DaemonCodeAnalyzer myDaemonCodeAnalyzer;

  @Inject
  public PsiExternalResourceNotifier(PsiManagerEx psiManager, ExternalResourceManager externalResourceManager,
                                     final DaemonCodeAnalyzer daemonCodeAnalyzer, Project project) {
    myPsiManager = psiManager;
    myExternalResourceManager = (ExternalResourceManagerEx)externalResourceManager;
    myDaemonCodeAnalyzer = daemonCodeAnalyzer;
    final ExternalResourceListener myExternalResourceListener = new MyExternalResourceListener();
    myExternalResourceManager.addExternalResourceListener(myExternalResourceListener);
    Disposer.register(project, new Disposable() {
      public void dispose() {
        myExternalResourceManager.removeExternalResourceListener(myExternalResourceListener);
      }
    });
  }

  private class MyExternalResourceListener implements ExternalResourceListener {
    public void externalResourceChanged() {
      myPsiManager.beforeChange(true);
      myDaemonCodeAnalyzer.restart();
    }
  }
}
