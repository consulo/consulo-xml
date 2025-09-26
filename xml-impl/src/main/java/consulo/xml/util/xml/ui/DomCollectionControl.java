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

import consulo.application.Application;
import consulo.application.Result;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.highlighting.DomCollectionProblemDescriptor;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;
import consulo.xml.util.xml.highlighting.DomElementProblemDescriptor;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.ui.actions.DefaultAddAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
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
    private List<T> myCollectionElements = new ArrayList<>();
    private ColumnInfo<T, ?>[] myColumnInfos;
    private boolean myEditable = false;
    public static final Image ADD_ICON = PlatformIconGroup.generalAdd();
    public static final Image EDIT_ICON = PlatformIconGroup.actionsEdit();
    public static final Image REMOVE_ICON = PlatformIconGroup.generalRemove();

    @SafeVarargs
    public DomCollectionControl(
        DomElement parentElement,
        DomCollectionChildDescription description,
        boolean editable,
        ColumnInfo<T, ?>... columnInfos
    ) {
        myChildDescription = description;
        myParentDomElement = parentElement;
        myColumnInfos = columnInfos;
        myEditable = editable;
    }

    @SafeVarargs
    public DomCollectionControl(
        DomElement parentElement,
        String subTagName,
        boolean editable,
        ColumnInfo<T, ?>... columnInfos
    ) {
        this(parentElement, parentElement.getGenericInfo().getCollectionChildDescription(subTagName), editable, columnInfos);
    }

    public DomCollectionControl(DomElement parentElement, DomCollectionChildDescription description) {
        myChildDescription = description;
        myParentDomElement = parentElement;
    }

    public DomCollectionControl(DomElement parentElement, String subTagName) {
        this(parentElement, parentElement.getGenericInfo().getCollectionChildDescription(subTagName));
    }

    public boolean isEditable() {
        return myEditable;
    }

    @Override
    public void bind(JComponent component) {
        assert component instanceof DomTableView;

        initialize((DomTableView) component);
    }

    @Override
    public void addCommitListener(CommitListener listener) {
        myDispatcher.addListener(listener);
    }

    @Override
    public void removeCommitListener(CommitListener listener) {
        myDispatcher.removeListener(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean canNavigate(DomElement element) {
        Class<DomElement> aClass = (Class<DomElement>) ReflectionUtil.getRawType(myChildDescription.getType());

        DomElement domElement = element.getParentOfType(aClass, false);

        return domElement != null && myCollectionElements.contains(domElement);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void navigate(DomElement element) {
        Class<DomElement> aClass = (Class<DomElement>) ReflectionUtil.getRawType(myChildDescription.getType());
        DomElement domElement = element.getParentOfType(aClass, false);

        int index = myCollectionElements.indexOf(domElement);
        if (index < 0) {
            index = 0;
        }

        myCollectionPanel.getTable().setRowSelectionInterval(index, index);
    }

    @Override
    public void calcData(Key<?> key, DataSink sink) {
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

    protected void initialize(DomTableView boundComponent) {
        if (boundComponent == null) {
            myCollectionPanel = new DomTableView(getProject(), getEmptyPaneText(), getHelpId());
        }
        else {
            myCollectionPanel = boundComponent;
        }
        myCollectionPanel.setToolbarActions(
            new DomCollectionControlAddAction(),
            new DomCollectionControlEditAction(),
            new DomCollectionControlRemoveAction()
        );
        myCollectionPanel.installPopup(ActionPlaces.J2EE_ATTRIBUTES_VIEW_POPUP, createPopupActionGroup());
        myCollectionPanel.initializeTable();
        myCollectionPanel.addCustomDataProvider(this);
        myCollectionPanel.addChangeListener(this::reset);
        reset();
    }

    protected DefaultActionGroup createPopupActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.addAll((DefaultActionGroup) ActionManager.getInstance().getAction("DomCollectionControl"));
        return group;
    }

    protected ColumnInfo[] createColumnInfos(DomElement parent) {
        return myColumnInfos;
    }

    protected final void doEdit() {
        doEdit(myCollectionElements.get(myCollectionPanel.getTable().getSelectedRow()));
    }

    protected void doEdit(T t) {
        DomEditorManager manager = getDomEditorManager(this);
        if (manager != null) {
            manager.openDomElementEditor(t);
        }
    }

    @RequiredUIAccess
    protected void doRemove(final List<T> toDelete) {
        Set<PsiFile> files = new HashSet<>();
        for (T t : toDelete) {
            XmlElement element = t.getXmlElement();
            if (element != null) {
                ContainerUtil.addIfNotNull(files, element.getContainingFile());
            }
        }

        new WriteCommandAction(getProject(), PsiUtilCore.toPsiFileArray(files)) {
            @Override
            protected void run(Result result) throws Throwable {
                for (T t : toDelete) {
                    if (t.isValid()) {
                        t.undefine();
                    }
                }
            }
        }.execute();
    }

    protected final void doRemove() {
        Application.get().invokeLater(() -> {
            int[] selected = myCollectionPanel.getTable().getSelectedRows();
            if (selected == null || selected.length == 0) {
                return;
            }
            List<T> selectedElements = new ArrayList<>(selected.length);
            for (int i : selected) {
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
        });
    }

    @RequiredUIAccess
    protected static void performWriteCommandAction(WriteCommandAction writeCommandAction) {
        writeCommandAction.execute();
    }

    @Override
    public void commit() {
        CommitListener listener = myDispatcher.getMulticaster();
        listener.beforeCommit(this);
        listener.afterCommit(this);
        validate();
    }

    private void validate() {
        DomElement domElement = getDomElement();
        List<DomElementProblemDescriptor> list =
            DomElementAnnotationsManager.getInstance(getProject()).getCachedProblemHolder(domElement).getProblems(domElement);
        List<String> messages = new ArrayList<>();
        for (DomElementProblemDescriptor descriptor : list) {
            if (descriptor instanceof DomCollectionProblemDescriptor domProblemDescriptor
                && myChildDescription.equals(domProblemDescriptor.getChildDescription())) {
                messages.add(descriptor.getDescriptionTemplate());
            }
        }
        myCollectionPanel.setErrorMessages(ArrayUtil.toStringArray(messages));
        myCollectionPanel.repaint();
    }

    @Override
    public void dispose() {
        if (myCollectionPanel != null) {
            myCollectionPanel.dispose();
        }
    }

    protected final Project getProject() {
        return myParentDomElement.getManager().getProject();
    }

    @Override
    public DomTableView getComponent() {
        if (myCollectionPanel == null) {
            initialize(null);
        }

        return myCollectionPanel;
    }

    public final DomCollectionChildDescription getChildDescription() {
        return myChildDescription;
    }

    @Override
    public final DomElement getDomElement() {
        return myParentDomElement;
    }

    @Override
    public final void reset() {
        myCollectionElements = new ArrayList<>(getCollectionElements());
        myCollectionPanel.reset(createColumnInfos(myParentDomElement), myCollectionElements);
        validate();
    }

    @SuppressWarnings("unchecked")
    public List<T> getCollectionElements() {
        return (List<T>) myChildDescription.getValues(myParentDomElement);
    }

    @Nullable
    protected AnAction[] createAdditionActions() {
        return null;
    }

    protected DefaultAddAction createDefaultAction(final String name, final Image icon, final Type type) {
        return new ControlAddAction(name, name, icon) {
            @Override
            protected Type getElementType() {
                return type;
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected final Class<? extends T> getCollectionElementClass() {
        return (Class<? extends T>) ReflectionUtil.getRawType(myChildDescription.getType());
    }

    @Nullable
    private static DomEditorManager getDomEditorManager(DomUIControl control) {
        JComponent component = control.getComponent();
        while (component != null && !(component instanceof DomEditorManager)) {
            if (!(component.getParent() instanceof JComponent jComponent)) {
                return null;
            }
            component = jComponent;
        }
        return (DomEditorManager) component;
    }

    @Override
    public void updateHighlighting() {
        if (myCollectionPanel != null) {
            myCollectionPanel.revalidate();
            myCollectionPanel.repaint();
        }
    }

    public class ControlAddAction extends DefaultAddAction<T> {
        public ControlAddAction() {
        }

        public ControlAddAction(String text) {
            super(text);
        }

        public ControlAddAction(String text, String description, Image icon) {
            super(text, description, icon);
        }

        @Override
        protected final DomCollectionChildDescription getDomCollectionChildDescription() {
            return myChildDescription;
        }

        @Override
        protected final DomElement getParentDomElement() {
            return myParentDomElement;
        }

        /**
         * return negative value to disable auto-edit
         */
        protected int getColumnToEditAfterAddition() {
            return 0;
        }

        protected void afterAddition(JTable table, int rowIndex) {
            table.setRowSelectionInterval(rowIndex, rowIndex);
            int column = getColumnToEditAfterAddition();
            if (column >= 0) {
                table.editCellAt(rowIndex, column);
            }
        }

        @Override
        protected final void afterAddition(@Nonnull T newElement) {
            reset();
            afterAddition(myCollectionPanel.getTable(), myCollectionElements.size() - 1);
        }
    }

    public static DomCollectionControl getDomCollectionControl(AnActionEvent e) {
        return e.getData(DOM_COLLECTION_CONTROL);
    }
}
