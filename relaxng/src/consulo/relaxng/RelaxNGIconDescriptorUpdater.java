package consulo.relaxng;

import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.jetbrains.annotations.NotNull;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import consulo.annotations.RequiredReadAction;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;

/**
 * @author VISTALL
 * @since 18-Jan-17
 */
public class RelaxNGIconDescriptorUpdater implements IconDescriptorUpdater
{
	@RequiredReadAction
	@Override
	public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement psiElement, int i)
	{
		if(psiElement instanceof RncDefine)
		{
			iconDescriptor.setMainIcon(AllIcons.Nodes.Property);
		}
	}
}
