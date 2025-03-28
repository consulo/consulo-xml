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
package com.intellij.xml.actions.validate;

import com.intellij.xml.XmlBundle;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.disposer.Disposer;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.view.MessageView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.relaxng.ContentManagerUtilHack;
import consulo.ui.ex.MessageCategory;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerAdapter;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanel;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanelFactory;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.xml.sax.SAXParseException;

import jakarta.annotation.Nonnull;
import java.util.concurrent.Future;

public class StdErrorReporter extends ErrorReporter {
    private static final Logger LOG = Logger.getInstance(StdErrorReporter.class);
    private static final Key<NewErrorTreeViewPanel> KEY = Key.create("ValidateXmlAction.KEY");

    private final NewErrorTreeViewPanel myErrorsView;
    private final String myContentName;
    private final Project myProject;

    public StdErrorReporter(ValidateXmlActionHandler handler, PsiFile psiFile, Runnable rerunAction) {
        super(handler);
        myProject = psiFile.getProject();
        myContentName = XmlBundle.message("xml.validate.tab.content.title", psiFile.getName());
        myErrorsView = Application.get().getInstance(NewErrorTreeViewPanelFactory.class)
            .createPanel(myProject, null, true, true, rerunAction);
        //myErrorsView.getEmptyText().setText("No errors found");
    }

    @Override
    public void startProcessing() {
        final MyProcessController processController = new MyProcessController();
        myErrorsView.setProcessController(processController);
        openMessageView();
        processController.setFuture(ApplicationManager.getApplication().executeOnPooledThread(
            () -> ApplicationManager.getApplication().runReadAction(() -> super.startProcessing())
        ));

        ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW).activate(null);
    }

    private void openMessageView() {
        CommandProcessor commandProcessor = CommandProcessor.getInstance();
        commandProcessor.executeCommand(
            myProject,
            () -> {
                MessageView messageView = MessageView.SERVICE.getInstance(myProject);
                final Content content =
                    ContentFactory.SERVICE.getInstance().createContent(myErrorsView.getComponent(), myContentName, true);
                content.putUserData(KEY, myErrorsView);
                messageView.getContentManager().addContent(content);
                messageView.getContentManager().setSelectedContent(content);
                messageView.getContentManager().addContentManagerListener(new CloseListener(content, messageView.getContentManager()));
                ContentManagerUtilHack.cleanupContents(content, myProject, myContentName);
                messageView.getContentManager().addContentManagerListener(new MyContentDisposer(content, messageView));
            },
            XmlBundle.message("validate.xml.open.message.view.command.name"),
            null
        );
    }

    @Override
    public void processError(final SAXParseException ex, final ValidateXmlActionHandler.ProblemType problemType) {
        if (LOG.isDebugEnabled()) {
            String error = myHandler.buildMessageString(ex);
            LOG.debug("enter: processError(error='" + error + "')");
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            final VirtualFile file = myHandler.getProblemFile(ex);
            myErrorsView.addMessage(
                problemType == ValidateXmlActionHandler.ProblemType.WARNING ? MessageCategory.WARNING : MessageCategory.ERROR,
                new String[]{ex.getLocalizedMessage()},
                file,
                ex.getLineNumber() - 1,
                ex.getColumnNumber() - 1,
                null
            );
        });
    }

    private static class MyContentDisposer implements ContentManagerListener {
        private final Content myContent;
        private final MessageView myMessageView;

        MyContentDisposer(final Content content, final MessageView messageView) {
            myContent = content;
            myMessageView = messageView;
        }

        @Override
        public void contentRemoved(@Nonnull ContentManagerEvent event) {
            final Content eventContent = event.getContent();
            if (!eventContent.equals(myContent)) {
                return;
            }
            myMessageView.getContentManager().removeContentManagerListener(this);
            NewErrorTreeViewPanel errorTreeView = eventContent.getUserData(KEY);
            if (errorTreeView != null) {
                Disposer.dispose(errorTreeView);
            }
            eventContent.putUserData(KEY, null);
        }

        @Override
        public void contentAdded(@Nonnull ContentManagerEvent event) {
        }

        @Override
        public void contentRemoveQuery(@Nonnull ContentManagerEvent event) {
        }

        @Override
        public void selectionChanged(@Nonnull ContentManagerEvent event) {
        }
    }

    private class CloseListener extends ContentManagerAdapter {
        private Content myContent;
        private final ContentManager myContentManager;

        CloseListener(Content content, ContentManager contentManager) {
            myContent = content;
            myContentManager = contentManager;
        }

        @Override
        public void contentRemoved(@Nonnull ContentManagerEvent event) {
            if (event.getContent() == myContent) {
                myErrorsView.stopProcess();

                myContentManager.removeContentManagerListener(this);
                myContent.release();
                myContent = null;
            }
        }

        @Override
        public void contentRemoveQuery(@Nonnull ContentManagerEvent event) {
            if (event.getContent() == myContent) {
                if (!myErrorsView.isProcessStopped()) {
                    int result = Messages.showYesNoDialog(
                        XmlBundle.message("xml.validate.validation.is.running.terminate.confirmation.text"),
                        XmlBundle.message("xml.validate.validation.is.running.terminate.confirmation.title"),
                        UIUtil.getQuestionIcon()
                    );
                    if (result != Messages.YES) {
                        event.consume();
                    }
                }
            }
        }
    }

    private static class MyProcessController implements NewErrorTreeViewPanel.ProcessController {
        private Future<?> myFuture;

        public void setFuture(Future<?> future) {
            myFuture = future;
        }

        @Override
        public void stopProcess() {
            if (myFuture != null) {
                myFuture.cancel(true);
            }
        }

        @Override
        public boolean isProcessStopped() {
            return myFuture != null && myFuture.isDone();
        }
    }
}
