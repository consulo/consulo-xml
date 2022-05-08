/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.xml.util.xml.ui;

import consulo.dataContext.DataSink;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.xml.XmlElement;
import consulo.proxy.EventDispatcher;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.CommonActionsPanel;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.highlighting.DomCollectionProblemDescriptor;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;
import consulo.xml.util.xml.highlighting.DomElementProblemDescriptor;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.ui.actions.AddDomElementAction;
import consulo.xml.util.xml.ui.actions.DefaultAddAction;
import consulo.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import consulo.application.Result;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author peter
 */
public class DomCollectionControl<T extends DomElement> extends DomUIControl implements Highlightable, TypeSafeDataProvider {
  private static final Key<DomCollectionControl> DOM_COLLECTION_CONTROL = Key.create("DomCollectionControl");

  private final EventDispatcher<CommitListener> myDispatcher = EventDispatcher.create(CommitListener.class);
  private DomTableView myCollectionPanel;

  private final DomElement myParentDomElement;
  private final DomCollectionChildDescription myChildDescription;
  private List<T> myCollectionElements = new ArrayList<T>();
  private ColumnInfo<T, ?>[] myColumnInfos;
  private boolean myEditable = false;
  public static final Image ADD_ICON = IconUtil.getAddIcon();
  public static final Image EDIT_ICON = IconUtil.getEditIcon();
  public static final Image REMOVE_ICON = IconUtil.getRemoveIcon();

  public DomCollectionControl(DomElement parentElement,
                              DomCollectionChildDescription description,
                              final boolean editable,
                              ColumnInfo<T, ?>... columnInfos) {
    myChildDescription = description;
    myParentDomElement = parentElement;
    myColumnInfos = columnInfos;
    myEditable = editable;
  }

  public DomCollectionControl(DomElement parentElement,
                              @NonNls String subTagName,
                              final boolean editable,
                              ColumnInfo<T, ?>... columnInfos) {
    this(parentElement, parentElement.getGenericInfo().getCollectionChildDescription(subTagName), editable, columnInfos);
  }

  public DomCollectionControl(DomElement parentElement, DomCollectionChildDescription description) {
    myChildDescription = description;
    myParentDomElement = parentElement;
  }

  public DomCollectionControl(DomElement parentElement, @NonNls String subTagName) {
    this(parentElement, parentElement.getGenericInfo().getCollectionChildDescription(subTagName));
  }

  public boolean isEditable() {
    return myEditable;
  }

  public void bind(JComponent component) {
    assert component instanceof DomTableView;

    initialize((DomTableView)component);
  }

  public void addCommitListener(CommitListener listener) {
    myDispatcher.addListener(listener);
  }

  public void removeCommitListener(CommitListener listener) {
    myDispatcher.removeListener(listener);
  }


  public boolean canNavigate(DomElement element) {
    final Class<DomElement> aClass = (Class<DomElement>) ReflectionUtil.getRawType(myChildDescription.getType());

    final DomElement domElement = element.getParentOfType(aClass, false);

    return domElement != null && myCollectionElements.contains(domElement);
  }

  public void navigate(DomElement element) {
    final Class<DomElement> aClass = (Class<DomElement>)ReflectionUtil.getRawType(myChildDescription.getType());
    final DomElement domElement = element.getParentOfType(aClass, false);

    int index = myCollectionElements.indexOf(domElement);
    if (index < 0) index = 0;

    myCollectionPanel.getTable().setRowSelectionInterval(index, index);
  }

  public void calcData(final Key<?> key, final DataSink sink) {
    if (DOM_COLLECTION_CONTROL.equals(key)) {
      sink.put(DOM_COLLECTION_CONTROL, this);
    }
  }

  @Nullable
  protected String getHelpId() {
    return null;
  }

  @Nullable
  protected String getEmptyPaneText() {
    return null;
  }

  protected void initialize(final DomTableView boundComponent) {
    if (boundComponent == null) {
      myCollectionPanel = new DomTableView(getProject(), getEmptyPaneText(), getHelpId());
    }
    else {
      myCollectionPanel = boundComponent;
    }
    myCollectionPanel.setToolbarActions(new AddAction(), new EditAction(), new RemoveAction());
    myCollectionPanel.installPopup(ActionPlaces.J2EE_ATTRIBUTES_VIEW_POPUP, createPopupActionGroup());
    myCollectionPanel.initializeTable();
    myCollectionPanel.addCustomDataProvider(this);
    myCollectionPanel.addChangeListener(new DomTableView.ChangeListener() {
      public void changed() {
        reset();
      }
    });
    reset();
  }

  protected DefaultActionGroup createPopupActionGroup() {
    final DefaultActionGroup group = new DefaultActionGroup();
    group.addAll((DefaultActionGroup) ActionManager.getInstance().getAction("DomCollectionControl"));
    return group;
  }

  protected ColumnInfo[] createColumnInfos(DomElement parent) {
    return myColumnInfos;
  }

  protected final void doEdit() {
    doEdit(myCollectionElements.get(myCollectionPanel.getTable().getSelectedRow()));
  }

  protected void doEdit(final T t) {
    final DomEditorManager manager = getDomEditorManager(this);
    if (manager != null) {
      manager.openDomElementEditor(t);
    }
  }

  protected void doRemove(final List<T> toDelete) {
    Set<PsiFile> files = new HashSet<PsiFile>();
    for (final T t : toDelete) {
      final XmlElement element = t.getXmlElement();
      if (element != null) {
        ContainerUtil.addIfNotNull(files, element.getContainingFile());
      }
    }

    new WriteCommandAction(getProject(), PsiUtilCore.toPsiFileArray(files)) {
      protected void run(Result result) throws Throwable {
        for (final T t : toDelete) {
          if (t.isValid()) {
            t.undefine();
          }
        }
      }
    }.execute();
  }

  protected final void doRemove() {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        final int[] selected = myCollectionPanel.getTable().getSelectedRows();
        if (selected == null || selected.length == 0) return;
        final List<T> selectedElements = new ArrayList<T>(selected.length);
        for (final int i : selected) {
          selectedElements.add(myCollectionElements.get(i));
        }

        doRemove(selectedElements);
        reset();
        int selection = selected[0];
        if (selection >= myCollectionElements.size()) {
          selection = myCollectionElements.size() - 1;
        }
        if (selection >= 0) {
          myCollectionPanel.getTable().setRowSelectionInterval(selection, selection);
        }
      }
    });
  }

  protected static void performWriteCommandAction(final WriteCommandAction writeCommandAction) {
    writeCommandAction.execute();
  }

  public void commit() {
    final CommitListener listener = myDispatcher.getMulticaster();
    listener.beforeCommit(this);
    listener.afterCommit(this);
    validate();
  }

  private void validate() {
    DomElement domElement = getDomElement();
    final List<DomElementProblemDescriptor> list =
      DomElementAnnotationsManager.getInstance(getProject()).getCachedProblemHolder(domElement).getProblems(domElement);
    final List<String> messages = new ArrayList<String>();
    for (final DomElementProblemDescriptor descriptor : list) {
      if (descriptor instanceof DomCollectionProblemDescriptor
          && myChildDescription.equals(((DomCollectionProblemDescriptor)descriptor).getChildDescription())) {
        messages.add(descriptor.getDescriptionTemplate());
      }
    }
    myCollectionPanel.setErrorMessages(ArrayUtil.toStringArray(messages));
    myCollectionPanel.repaint();
  }

  public void dispose() {
    if (myCollectionPanel != null) {
      myCollectionPanel.dispose();
    }
  }

  protected final Project getProject() {
    return myParentDomElement.getManager().getProject();
  }

  public DomTableView getComponent() {
    if (myCollectionPanel == null) initialize(null);

    return myCollectionPanel;
  }

  public final DomCollectionChildDescription getChildDescription() {
    return myChildDescription;
  }

  public final DomElement getDomElement() {
    return myParentDomElement;
  }

  public final void reset() {
    myCollectionElements = new ArrayList<T>(getCollectionElements());
    myCollectionPanel.reset(createColumnInfos(myParentDomElement), myCollectionElements);
    validate();
  }

  public List<T> getCollectionElements() {
    return (List<T>)myChildDescription.getValues(myParentDomElement);
  }

  @Nullable
  protected AnAction[] createAdditionActions() {
    return null;
  }

  protected DefaultAddAction createDefaultAction(final String name, final Image icon, final Type type) {
    return new ControlAddAction(name, name, icon) {
      protected Type getElementType() {
        return type;
      }
    };
  }

  protected final Class<? extends T> getCollectionElementClass() {
    return (Class<? extends T>) ReflectionUtil.getRawType(myChildDescription.getType());
  }


  @Nullable
  private static DomEditorManager getDomEditorManager(DomUIControl control) {
    JComponent component = control.getComponent();
    while (component != null && !(component instanceof DomEditorManager)) {
      final Container parent = component.getParent();
      if (!(parent instanceof JComponent)) {
        return null;
      }
      component = (JComponent)parent;
    }
    return (DomEditorManager)component;
  }

  public void updateHighlighting() {
    if (myCollectionPanel != null) {
      myCollectionPanel.revalidate();
      myCollectionPanel.repaint();
    }
  }

  public class ControlAddAction extends DefaultAddAction<T> {

    public ControlAddAction() {
    }

    public ControlAddAction(final String text) {
      super(text);
    }

    public ControlAddAction(final String text, final String description, final Image icon) {
      super(text, description, icon);
    }

    protected final DomCollectionChildDescription getDomCollectionChildDescription() {
      return myChildDescription;
    }

    protected final DomElement getParentDomElement() {
      return myParentDomElement;
    }

    /**
     * return negative value to disable auto-edit
     * @return
     */
    protected int getColumnToEditAfterAddition() {
      return 0;
    }

    protected void afterAddition(final JTable table, final int rowIndex) {
      table.setRowSelectionInterval(rowIndex, rowIndex);
      final int column = getColumnToEditAfterAddition();
      if (column >= 0 ) {
        table.editCellAt(rowIndex, column);
      }
    }

    protected final void afterAddition(final T newElement) {
      reset();
      afterAddition(myCollectionPanel.getTable(), myCollectionElements.size() - 1);
    }
  }

  public static DomCollectionControl getDomCollectionControl(final AnActionEvent e) {
    return e.getData(DOM_COLLECTION_CONTROL);
  }

  public static class AddAction extends AddDomElementAction {

    public AddAction() {
      setShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD));
    }

    protected boolean isEnabled(final AnActionEvent e) {
      return getDomCollectionControl(e) != null;
    }

    protected DomCollectionControl getDomCollectionControl(final AnActionEvent e) {
      return DomCollectionControl.getDomCollectionControl(e);
    }

    @Nonnull
    protected DomCollectionChildDescription[] getDomCollectionChildDescriptions(final AnActionEvent e) {
      return new DomCollectionChildDescription[] {getDomCollectionControl(e).getChildDescription()};
    }

    protected DomElement getParentDomElement(final AnActionEvent e) {
      return getDomCollectionControl(e).getDomElement();
    }

    protected JComponent getComponent(AnActionEvent e) {
      return getDomCollectionControl(e).getComponent();
    }

    @Nonnull
    public AnAction[] getChildren(final AnActionEvent e) {
      final DomCollectionControl control = getDomCollectionControl(e);
      AnAction[] actions = control.createAdditionActions();
      return actions == null ? super.getChildren(e) : actions;
    }

    protected DefaultAddAction createAddingAction(final AnActionEvent e,
                                                  final String name,
                                                  final Image icon,
                                                  final Type type,
                                                  final DomCollectionChildDescription description) {
      return getDomCollectionControl(e).createDefaultAction(name, icon, type);
    }

  }

  public static class EditAction extends AnAction {

    public EditAction() {
      super(ApplicationBundle.message("action.edit"), null, DomCollectionControl.EDIT_ICON);
      setShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.EDIT));
    }

    public void actionPerformed(AnActionEvent e) {
      final DomCollectionControl control = DomCollectionControl.getDomCollectionControl(e);
      control.doEdit();
      control.reset();
    }

    public void update(AnActionEvent e) {
      final DomCollectionControl control = DomCollectionControl.getDomCollectionControl(e);
      final boolean visible = control != null && control.isEditable();
      e.getPresentation().setVisible(visible);
      e.getPresentation().setEnabled(visible && control.getComponent().getTable().getSelectedRowCount() == 1);
    }
  }

  public static class RemoveAction extends AnAction {
    public RemoveAction() {
      super(ApplicationBundle.message("action.remove"), null, DomCollectionControl.REMOVE_ICON);
      setShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.REMOVE));
    }

    public void actionPerformed(AnActionEvent e) {
      final DomCollectionControl control = DomCollectionControl.getDomCollectionControl(e);
      control.doRemove();
      control.reset();
    }

    public void update(AnActionEvent e) {
      final boolean enabled;
      final DomCollectionControl control = DomCollectionControl.getDomCollectionControl(e);
      if (control != null) {
        final JTable table = control.getComponent().getTable();
        enabled = table != null && table.getSelectedRowCount() > 0;
      } else {
        enabled = false;
      }
      e.getPresentation().setEnabled(enabled);
    }
  }
}
