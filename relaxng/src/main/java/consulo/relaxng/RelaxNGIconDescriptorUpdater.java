package consulo.relaxng;

import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import javax.annotation.Nonnull;
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
	public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement psiElement, int i)
	{
		if(psiElement instanceof RncDefine)
		{
			iconDescriptor.setMainIcon(AllIcons.Nodes.Property);
		}
	}
}
