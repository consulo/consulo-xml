package consulo.relaxng;

import consulo.project.Project;
import consulo.project.ui.view.MessageView;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.errorTreeView.ErrorTreeView;

public class ContentManagerUtilHack {
    public static void cleanupContents(Content notToRemove, Project project, String contentName) {
        MessageView messageView = MessageView.SERVICE.getInstance(project);

        for (Content content : messageView.getContentManager().getContents()) {
            if (content.isPinned()) {
                continue;
            }
            if (contentName.equals(content.getDisplayName()) && content != notToRemove) {
                ErrorTreeView listErrorView = (ErrorTreeView)content.getComponent();
                if (listErrorView != null && messageView.getContentManager().removeContent(content, true)) {
                    content.release();
                }
            }
        }
    }
}
