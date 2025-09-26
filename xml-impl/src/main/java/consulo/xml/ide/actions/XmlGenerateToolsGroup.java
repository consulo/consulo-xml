package consulo.xml.ide.actions;

import com.intellij.xml.actions.xmlbeans.GenerateInstanceDocumentFromSchemaAction;
import com.intellij.xml.actions.xmlbeans.GenerateSchemaFromInstanceDocumentAction;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.xml.codeInsight.actions.GenerateDTDAction;

/**
 * @author UNV
 * @since 2025-09-25
 */
@ActionImpl(
    id = "XmlGenerateToolsGroup",
    children = {
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = GenerateDTDAction.class),
        @ActionRef(type = GenerateSchemaFromInstanceDocumentAction.class),
        @ActionRef(type = GenerateInstanceDocumentFromSchemaAction.class),
        @ActionRef(type = AnSeparator.class)
    },
    parents = {
        @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_EDITOR_POPUP)),
        @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_PROJECT_VIEW_POPUP))
    }
)
public class XmlGenerateToolsGroup extends DefaultActionGroup implements DumbAware {
    public XmlGenerateToolsGroup() {
        super(LocalizeValue.empty(), false);
    }
}
