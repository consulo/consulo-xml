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

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerEx;
import consulo.disposer.Disposable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
public class PsiExternalResourceNotifier implements Disposable
{
	private final PsiManager myPsiManager;
	private final ExternalResourceManagerEx myExternalResourceManager;
	private final DaemonCodeAnalyzer myDaemonCodeAnalyzer;
	private final ExternalResourceListener myExternalResourceListener;

	@Inject
	public PsiExternalResourceNotifier(PsiManager psiManager, ExternalResourceManager externalResourceManager, final DaemonCodeAnalyzer daemonCodeAnalyzer)
	{
		myPsiManager = psiManager;
		myExternalResourceManager = (ExternalResourceManagerEx) externalResourceManager;
		myDaemonCodeAnalyzer = daemonCodeAnalyzer;

		myExternalResourceListener = new MyExternalResourceListener();
		myExternalResourceManager.addExternalResourceListener(myExternalResourceListener);
	}

	private class MyExternalResourceListener implements ExternalResourceListener
	{
		public void externalResourceChanged()
		{
			((PsiManagerEx)myPsiManager).beforeChange(true);
			myDaemonCodeAnalyzer.restart();
		}
	}

	@Override
	public void dispose()
	{
		myExternalResourceManager.removeExternalResourceListener(myExternalResourceListener);
	}
}
