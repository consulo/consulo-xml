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

import java.awt.event.InputEvent;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JList;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.OpenInBrowserRequest;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserService;
import com.intellij.ide.browsers.WebBrowserUrlProvider;
import com.intellij.ide.browsers.impl.WebBrowserServiceImpl;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.components.JBList;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Url;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.util.HtmlUtil;
import consulo.ui.image.Image;

public abstract class BaseOpenInBrowserAction extends DumbAwareAction
{
	private static final Logger LOG = Logger.getInstance(BaseOpenInBrowserAction.class);

	protected BaseOpenInBrowserAction(@Nonnull WebBrowser browser)
	{
		super(browser.getName(), null, browser.getIcon());
	}

	@SuppressWarnings("UnusedDeclaration")
	protected BaseOpenInBrowserAction(@Nullable String text, @Nullable String description, @Nullable Image icon)
	{
		super(text, description, icon);
	}

	@Nullable
	protected abstract WebBrowser getBrowser(@Nonnull AnActionEvent event);

	@Override
	public final void update(AnActionEvent e)
	{
		WebBrowser browser = getBrowser(e);
		if(browser == null)
		{
			e.getPresentation().setEnabledAndVisible(false);
			return;
		}

		Pair<OpenInBrowserRequest, WebBrowserUrlProvider> result = doUpdate(e);
		if(result == null)
		{
			return;
		}

		String description = getTemplatePresentation().getText();
		if(ActionPlaces.CONTEXT_TOOLBAR.equals(e.getPlace()))
		{
			StringBuilder builder = new StringBuilder(description);
			builder.append(" (");
			Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts("WebOpenInAction");
			boolean exists = shortcuts.length > 0;
			if(exists)
			{
				builder.append(KeymapUtil.getShortcutText(shortcuts[0]));
			}

			if(HtmlUtil.isHtmlFile(result.first.getFile()))
			{
				builder.append(exists ? ", " : "").append("hold Shift to open URL of local file");
			}
			builder.append(')');
			description = builder.toString();
		}
		e.getPresentation().setText(description);
	}

	@Override
	public final void actionPerformed(AnActionEvent e)
	{
		WebBrowser browser = getBrowser(e);
		if(browser != null)
		{
			open(e, browser);
		}
	}

	@Nullable
	public static OpenInBrowserRequest createRequest(@Nonnull DataContext context)
	{
		final Editor editor = context.getData(CommonDataKeys.EDITOR);
		if(editor != null)
		{
			Project project = editor.getProject();
			if(project != null && project.isInitialized())
			{
				PsiFile psiFile = context.getData(CommonDataKeys.PSI_FILE);
				if(psiFile == null)
				{
					psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
				}
				if(psiFile != null)
				{
					return new OpenInBrowserRequest(psiFile)
					{
						private PsiElement element;

						@Nonnull
						@Override
						public PsiElement getElement()
						{
							if(element == null)
							{
								element = getFile().findElementAt(editor.getCaretModel().getOffset());
							}
							return ObjectUtils.chooseNotNull(element, getFile());
						}
					};
				}
			}
		}
		else
		{
			PsiFile psiFile = context.getData(CommonDataKeys.PSI_FILE);
			VirtualFile virtualFile = context.getData(CommonDataKeys.VIRTUAL_FILE);
			Project project = context.getData(CommonDataKeys.PROJECT);
			if(virtualFile != null && !virtualFile.isDirectory() && virtualFile.isValid() && project != null && project.isInitialized())
			{
				psiFile = PsiManager.getInstance(project).findFile(virtualFile);
			}

			if(psiFile != null)
			{
				return OpenInBrowserRequest.create(psiFile);
			}
		}
		return null;
	}

	@Nullable
	public static Pair<OpenInBrowserRequest, WebBrowserUrlProvider> doUpdate(@Nonnull AnActionEvent event)
	{
		OpenInBrowserRequest request = createRequest(event.getDataContext());
		boolean applicable = false;
		WebBrowserUrlProvider provider = null;
		if(request != null)
		{
			applicable = HtmlUtil.isHtmlFile(request.getFile()) && !(request.getVirtualFile() instanceof LightVirtualFile);
			if(!applicable)
			{
				provider = WebBrowserServiceImpl.getProvider(request);
				applicable = provider != null;
			}
		}

		event.getPresentation().setEnabledAndVisible(applicable);
		return applicable ? Pair.create(request, provider) : null;
	}

	public static void open(@Nonnull AnActionEvent event, @Nullable WebBrowser browser)
	{
		open(createRequest(event.getDataContext()), (event.getModifiers() & InputEvent.SHIFT_MASK) != 0, browser);
	}

	public static void open(@Nullable final OpenInBrowserRequest request, boolean preferLocalUrl, @Nullable final WebBrowser browser)
	{
		if(request == null)
		{
			return;
		}

		try
		{
			Collection<Url> urls = WebBrowserService.getInstance().getUrlsToOpen(request, preferLocalUrl);
			if(!urls.isEmpty())
			{
				chooseUrl(urls).doWhenDone(url -> {
					ApplicationManager.getApplication().saveAll();
					BrowserLauncher.getInstance().browse(url.toExternalForm(), browser, request.getProject());
				});
			}
		}
		catch(WebBrowserUrlProvider.BrowserException e1)
		{
			Messages.showErrorDialog(e1.getMessage(), IdeBundle.message("browser.error"));
		}
		catch(Exception e1)
		{
			LOG.error(e1);
		}
	}

	@Nonnull
	private static AsyncResult<Url> chooseUrl(@Nonnull Collection<Url> urls)
	{
		if(urls.size() == 1)
		{
			return AsyncResult.resolved(ContainerUtil.getFirstItem(urls));
		}

		final JBList list = new JBList(urls);
		list.setCellRenderer(new ColoredListCellRenderer()
		{
			@Override
			protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus)
			{
				// todo icons looks good, but is it really suitable for all URLs providers?
				setIcon(AllIcons.Nodes.Servlet);
				append(((Url) value).toDecodedForm());
			}
		});

		final AsyncResult<Url> result = AsyncResult.undefined();
		JBPopupFactory.getInstance().
				createListPopupBuilder(list).
				setTitle("Choose Url").
				setItemChoosenCallback(new Runnable()
				{
					@Override
					public void run()
					{
						Url value = (Url) list.getSelectedValue();
						if(value != null)
						{
							result.setDone(value);
						}
						else
						{
							result.setRejected();
						}
					}
				}).
				createPopup().showInFocusCenter();
		return result;
	}
}