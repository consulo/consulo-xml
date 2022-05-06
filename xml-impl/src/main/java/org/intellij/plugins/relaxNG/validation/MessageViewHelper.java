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

package org.intellij.plugins.relaxNG.validation;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.view.MessageView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.MessageCategory;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.event.ContentManagerAdapter;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.errorTreeView.ErrorTreeView;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanel;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanelFactory;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 19.11.2007
 */
public class MessageViewHelper {
  private static final Logger LOG = Logger.getInstance(MessageViewHelper.class);

  private final Project myProject;

  private final Set<String> myErrors = new HashSet<>();

  private final String myContentName;
  private final Key<NewErrorTreeViewPanel> myKey;

  private NewErrorTreeViewPanel myErrorsView;
  private NewErrorTreeViewPanel.ProcessController myProcessController = MyProcessController.INSTANCE;

  public MessageViewHelper(Project project, String contentName, Key<NewErrorTreeViewPanel> key) {
    myProject = project;
    myContentName = contentName;
    myKey = key;
  }

  public synchronized void setProcessController(NewErrorTreeViewPanel.ProcessController processController) {
    if (myErrorsView == null) {
      myProcessController = processController;
    } else {
      myErrorsView.setProcessController(processController);
    }
  }

  public synchronized void openMessageView(Runnable rerun) {
    assert myErrorsView == null;
    myErrorsView = Application.get().getInstance(NewErrorTreeViewPanelFactory.class).createPanel(myProject, null, true, true, rerun);
    openMessageViewImpl();
  }

  public synchronized void processError(final SAXParseException ex, final boolean warning) {
    if (myErrors.size() == 0 && myErrorsView == null) {
      myErrorsView = Application.get().getInstance(NewErrorTreeViewPanelFactory.class).createPanel(myProject, null, true, true, null);
      myErrorsView.setProcessController(myProcessController);
      openMessageViewImpl();
    }
    final String error = ex.getLineNumber() + "|" + ex.getColumnNumber() + "|" + ex.getSystemId() + "|" + ex.getLocalizedMessage();
    if (!myErrors.add(error)) {
      return;
    }

    VirtualFile file = null;
    final String systemId = ex.getSystemId();
    if (systemId != null) {
      try {
        file = VfsUtil.findFileByURL(new URL(systemId));
      } catch (MalformedURLException e) {
        LOG.warn("systemId = " + systemId);
        LOG.error(e);
      }
    }

    final VirtualFile file1 = file;
    ApplicationManager.getApplication().invokeLater(
      () -> myErrorsView.addMessage(
        warning ? MessageCategory.WARNING : MessageCategory.ERROR,
        new String[]{ex.getLocalizedMessage()},
        file1,
        ex.getLineNumber() - 1,
        ex.getColumnNumber() - 1, null)
    );
  }

  public void close() {
    removeOldContents(null);
  }

  private void removeOldContents(Content notToRemove) {
    MessageView messageView = MessageView.SERVICE.getInstance(myProject);

    for (Content content : messageView.getContentManager().getContents()) {
      if (content.isPinned()) continue;
      if (myContentName.equals(content.getDisplayName()) && content != notToRemove) {
        ErrorTreeView listErrorView = (ErrorTreeView)content.getComponent();
        if (listErrorView != null) {
          if (messageView.getContentManager().removeContent(content, true)) {
            content.release();
          }
        }
      }
    }
  }

  private void openMessageViewImpl() {
    CommandProcessor commandProcessor = CommandProcessor.getInstance();
    commandProcessor.executeCommand(myProject, () -> {
      MessageView messageView = MessageView.SERVICE.getInstance(myProject);
      Content content = ContentFactory.SERVICE.getInstance().createContent(myErrorsView.getComponent(), myContentName, true);
      content.putUserData(myKey, myErrorsView);
      messageView.getContentManager().addContent(content);
      messageView.getContentManager().setSelectedContent(content);
      messageView.getContentManager().addContentManagerListener(new CloseListener(content, myContentName, myErrorsView));
      removeOldContents(content);
      messageView.getContentManager().addContentManagerListener(new MyContentDisposer(content, messageView, myKey));
    }, "Open Message View", null);

    ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW).activate(null);
  }

  private static class MyProcessController implements NewErrorTreeViewPanel.ProcessController {
    public static final MyProcessController INSTANCE = new MyProcessController();

    @Override
    public void stopProcess() {
    }

    @Override
    public boolean isProcessStopped() {
      return true;
    }
  }

  private static class CloseListener extends ContentManagerAdapter {
    private final String myContentName;

    private NewErrorTreeViewPanel myErrorsView;
    private Content myContent;

    public CloseListener(Content content, String contentName, NewErrorTreeViewPanel errorsView) {
      myContent = content;
      myContentName = contentName;
      myErrorsView = errorsView;
    }

    @Override
    public void contentRemoved(ContentManagerEvent event) {
      if (event.getContent() == myContent) {
        if (myErrorsView.canControlProcess()) {
          myErrorsView.stopProcess();
        }
        myErrorsView = null;

        myContent.getManager().removeContentManagerListener(this);
        myContent.release();
        myContent = null;
      }
    }

    @Override
    public void contentRemoveQuery(ContentManagerEvent event) {
      if (event.getContent() == myContent) {
        if (myErrorsView != null && myErrorsView.canControlProcess() && !myErrorsView.isProcessStopped()) {
          int result = Messages.showYesNoDialog(
            myContentName + " Running",
            myContentName + " is still running. Close anyway?",
              Messages.getQuestionIcon()
          );
          if (result != Messages.YES) {
            event.consume();
          }
        }
      }
    }
  }

  private static class MyContentDisposer extends ContentManagerAdapter {
    private final Content myContent;
    private final MessageView myMessageView;
    private final Key<NewErrorTreeViewPanel> myKey;

    MyContentDisposer(final Content content, final MessageView messageView, Key<NewErrorTreeViewPanel> key) {
      myContent = content;
      myMessageView = messageView;
      myKey = key;
    }

    @Override
    public void contentRemoved(ContentManagerEvent event) {
      final Content eventContent = event.getContent();
      if (!eventContent.equals(myContent)) {
        return;
      }
      myMessageView.getContentManager().removeContentManagerListener(this);
      NewErrorTreeViewPanel errorTreeView = eventContent.getUserData(myKey);
      if (errorTreeView != null) {
        errorTreeView.dispose();
      }
      eventContent.putUserData(myKey, null);
    }
  }

  public class ErrorHandler extends DefaultHandler {
    private boolean myHadErrorOrWarning;

    @Override
    public void warning(SAXParseException e) throws SAXException {
      myHadErrorOrWarning = true;
      processError(e, true);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      myHadErrorOrWarning = true;
      processError(e, false);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      myHadErrorOrWarning = true;
      processError(e, false);
    }

    public boolean hadErrorOrWarning() {
      return myHadErrorOrWarning;
    }
  }
}
