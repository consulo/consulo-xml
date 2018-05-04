/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.browsers.actions;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ComputableActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.psi.util.CachedValueProvider;

public abstract class OpenInBrowserBaseGroupAction extends ComputableActionGroup
{
	private OpenFileInDefaultBrowserAction myDefaultBrowserAction;

	protected OpenInBrowserBaseGroupAction(boolean popup)
	{
		super(popup);

		Presentation p = getTemplatePresentation();
		p.setText("Open in _Browser");
		p.setDescription("Open selected file in browser");
		p.setIcon(AllIcons.Nodes.PpWeb);
	}

	@Nonnull
	@Override
	protected final CachedValueProvider<AnAction[]> createChildrenProvider(@Nonnull final ActionManager actionManager)
	{
		return new CachedValueProvider<AnAction[]>()
		{
			@Nullable
			@Override
			public Result<AnAction[]> compute()
			{
				List<WebBrowser> browsers = WebBrowserManager.getInstance().getBrowsers();
				boolean addDefaultBrowser = isPopup();
				int offset = addDefaultBrowser ? 1 : 0;
				AnAction[] actions = new AnAction[browsers.size() + offset];

				if(addDefaultBrowser)
				{
					if(myDefaultBrowserAction == null)
					{
						myDefaultBrowserAction = new OpenFileInDefaultBrowserAction();
						myDefaultBrowserAction.getTemplatePresentation().setText("Default");
						myDefaultBrowserAction.getTemplatePresentation().setIcon(AllIcons.Nodes.PpWeb);
					}
					actions[0] = myDefaultBrowserAction;
				}

				for(int i = 0, size = browsers.size(); i < size; i++)
				{
					actions[i + offset] = new BaseWebBrowserAction(browsers.get(i));
				}

				return Result.create(actions, WebBrowserManager.getInstance());
			}
		};
	}

	public static final class OpenInBrowserGroupAction extends OpenInBrowserBaseGroupAction
	{
		public OpenInBrowserGroupAction()
		{
			super(true);
		}
	}

	public static final class OpenInBrowserEditorContextBarGroupAction extends OpenInBrowserBaseGroupAction
	{
		public OpenInBrowserEditorContextBarGroupAction()
		{
			super(false);
		}

		@Override
		public void update(@Nonnull AnActionEvent e)
		{
			e.getPresentation().setVisible(!WebBrowserManager.getInstance().getBrowsers().isEmpty());
		}
	}
}