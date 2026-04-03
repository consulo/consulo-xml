package consulo.relaxng;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;


/**
 * @author VISTALL
 * @since 18-Jan-17
 */
@ExtensionImpl
public class RelaxNGIconDescriptorUpdater implements IconDescriptorUpdater {
    @RequiredReadAction
    @Override
    public void updateIcon(IconDescriptor iconDescriptor, PsiElement psiElement, int i) {
        if (psiElement instanceof RncDefine) {
            iconDescriptor.setMainIcon(AllIcons.Nodes.Property);
        }
    }
}
