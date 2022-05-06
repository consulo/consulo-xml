package consulo.relaxng;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.MessageView;
import com.intellij.util.ui.ErrorTreeView;

public class ContentManagerUtilHack
{
	public static void cleanupContents(Content notToRemove, Project project, String contentName)
	{
		MessageView messageView = MessageView.SERVICE.getInstance(project);

		for(Content content : messageView.getContentManager().getContents())
		{
			if(content.isPinned())
				continue;
			if(contentName.equals(content.getDisplayName()) && content != notToRemove)
			{
				ErrorTreeView listErrorView = (ErrorTreeView) content.getComponent();
				if(listErrorView != null)
				{
					if(messageView.getContentManager().removeContent(content, true))
					{
						content.release();
					}
				}
			}
		}
	}
}
