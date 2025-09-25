package consulo.xml.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.NonEmptyActionGroup;
import consulo.xml.impl.localize.XmlLocalize;

/**
 * @author UNV
 * @since 2025-09-25
 */
@ActionImpl(id = "NewXmlDescriptor", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_NEW)))
public class NewXmlDescriptorGroup extends NonEmptyActionGroup implements DumbAware {
    public NewXmlDescriptorGroup() {
        super(XmlLocalize.groupNewXmlDescriptorText(), true);
        getTemplatePresentation().setIcon(PlatformIconGroup.filetypesXml());
    }
}
