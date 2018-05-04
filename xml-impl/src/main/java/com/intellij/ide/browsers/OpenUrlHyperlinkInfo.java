/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.browsers;

import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.execution.filters.HyperlinkWithPopupMenuInfo;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;

public final class OpenUrlHyperlinkInfo implements HyperlinkWithPopupMenuInfo
{
	private final String url;
	private final WebBrowser browser;
	private final Condition<WebBrowser> browserCondition;

	public OpenUrlHyperlinkInfo(@Nonnull String url)
	{
		this(url, Conditions.<WebBrowser>alwaysTrue(), null);
	}

	public OpenUrlHyperlinkInfo(@Nonnull String url, @Nullable WebBrowser browser)
	{
		this(url, null, browser);
	}

	public OpenUrlHyperlinkInfo(@Nonnull String url, @Nonnull Condition<WebBrowser> browserCondition)
	{
		this(url, browserCondition, null);
	}

	private OpenUrlHyperlinkInfo(@Nonnull String url, @Nullable Condition<WebBrowser> browserCondition, @Nullable WebBrowser browser)
	{
		this.url = url;
		this.browserCondition = browserCondition;
		this.browser = browser;
	}

	@Override
	public ActionGroup getPopupMenuGroup(@Nonnull MouseEvent event)
	{
		DefaultActionGroup group = new DefaultActionGroup();
		for(final WebBrowser browser : WebBrowserManager.getInstance().getActiveBrowsers())
		{
			if(browserCondition == null ? (this.browser == null || browser.equals(this.browser)) : browserCondition.value(browser))
			{
				group.add(new AnAction("Open in " + browser.getName(), "Open URL in " + browser.getName(), browser.getIcon())
				{
					@Override
					public void actionPerformed(AnActionEvent e)
					{
						BrowserLauncher.getInstance().browse(url, browser, e.getProject());
					}
				});
			}
		}

		group.add(new AnAction("Copy URL", "Copy URL to clipboard", AllIcons.Actions.Copy)
		{
			@Override
			public void actionPerformed(AnActionEvent e)
			{
				CopyPasteManager.getInstance().setContents(new StringSelection(url));
			}
		});
		return group;
	}

	@Override
	public void navigate(Project project)
	{
		BrowserLauncher.getInstance().browse(url, browser, project);
	}
}
