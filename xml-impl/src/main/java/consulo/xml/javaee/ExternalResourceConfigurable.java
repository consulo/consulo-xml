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
package consulo.xml.javaee;

import com.intellij.xml.XmlBundle;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.configurable.BaseConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.AddEditRemovePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class ExternalResourceConfigurable extends BaseConfigurable implements ProjectConfigurable, Configurable.NoScroll {
    private JPanel myPanel;
    private List<NameLocationPair> myPairs;
    private List<String> myIgnoredUrls;
    private String myDefaultHtmlDoctype;
    private AddEditRemovePanel<NameLocationPair> myExtPanel;
    private AddEditRemovePanel<String> myIgnorePanel;
    private HtmlLanguageLevelForm myHtmlLanguageLevelForm;
    @Nullable
    private final Project myProject;
    private final List<NameLocationPair> myNewPairs;

    @Inject
    public ExternalResourceConfigurable(@Nullable Project project) {
        this(project, List.of());
    }

    public ExternalResourceConfigurable(@Nullable Project project, List<NameLocationPair> newResources) {
        myProject = project;
        myNewPairs = newResources;
    }

    @Nonnull
    @Override
    public String getId() {
        return "preferences.externalResources";
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    @Override
    public String getDisplayName() {
        return XmlBundle.message("display.name.edit.external.resource");
    }

    @Override
    public JComponent createComponent(@Nonnull Disposable uiDisposable) {
        myPanel = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(-1, 400);
            }
        };

        myExtPanel = new AddEditRemovePanel<>(new ExtUrlsTableModel(), myPairs, XmlBundle.message("label.edit.external.resource.configure.external.resources")) {
            @Override
            protected NameLocationPair addItem() {
                return addExtLocation();
            }

            @Override
            protected boolean removeItem(NameLocationPair o) {
                setModified(true);
                return true;
            }

            @Override
            protected NameLocationPair editItem(NameLocationPair o) {
                return editExtLocation(o);
            }
        };

        myExtPanel.setRenderer(1, new PathRenderer());

        JTable table = myExtPanel.getTable();
        if (myProject != null) {
            TableColumn column = table.getColumn(table.getColumnName(2));
            column.setMaxWidth(50);
            column.setCellEditor(JBTable.createBooleanEditor());
        }

        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                setModified(true);
            }
        });
        myIgnorePanel = new AddEditRemovePanel<>(new IgnoredUrlsModel(), myIgnoredUrls, XmlBundle.message("label.edit.external.resource.configure.ignored.resources")) {
            @Override
            protected String addItem() {
                return addIgnoreLocation();
            }

            @Override
            protected boolean removeItem(String o) {
                setModified(true);
                return true;
            }

            @Override
            protected String editItem(String o) {
                return editIgnoreLocation(o);
            }
        };
        if (myProject != null) {
            myHtmlLanguageLevelForm = new HtmlLanguageLevelForm();
            myHtmlLanguageLevelForm.addListener(() ->
            {
                if (!myHtmlLanguageLevelForm.getDoctype().equals(myDefaultHtmlDoctype)) {
                    setModified(true);
                }
            });
        }

        myPanel.add(myExtPanel,
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        myPanel.add(myIgnorePanel,
            new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        if (myProject != null) {
            myPanel.add(TargetAWT.to(myHtmlLanguageLevelForm.getContentPanel()),
                new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        }

        myExtPanel.setData(myPairs);
        myIgnorePanel.setData(myIgnoredUrls);

        myExtPanel.getEmptyText().setText(XmlBundle.message("no.external.resources"));
        myIgnorePanel.getEmptyText().setText(XmlBundle.message("no.ignored.resources"));

        return myPanel;
    }

    @RequiredUIAccess
    @Override
    public void apply() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ExternalResourceManagerEx manager = ExternalResourceManagerEx.getInstanceEx();

                if (myProject == null) {
                    manager.clearAllResources();
                }
                else {
                    manager.clearAllResources(myProject);
                }
                for (NameLocationPair pair : myPairs) {
                    String s = FileUtil.toSystemIndependentName(StringUtil.notNullize(pair.myLocation));
                    if (myProject == null || pair.myShared) {
                        manager.addResource(pair.myName, s);
                    }
                    else {
                        manager.addResource(pair.myName, s, myProject);
                    }
                }

                for (Object myIgnoredUrl : myIgnoredUrls) {
                    String url = (String) myIgnoredUrl;
                    manager.addIgnoredResource(url);
                }
                if (myProject != null) {
                    manager.setDefaultHtmlDoctype(myHtmlLanguageLevelForm.getDoctype(), myProject);
                }
            }
        });

        setModified(false);
    }

    @Override
    public void reset() {

        myPairs = new ArrayList<>(myNewPairs);
        ExternalResourceManagerEx manager = ExternalResourceManagerEx.getInstanceEx();

        String[] urls = manager.getAvailableUrls();
        for (String url : urls) {
            String loc = myProject == null ? manager.getResourceLocation(url, (String) null) : manager.getResourceLocation(url, myProject);
            myPairs.add(new NameLocationPair(url, FileUtil.toSystemDependentName(loc), true));
        }
        if (myProject != null) {
            urls = manager.getAvailableUrls(myProject);
            for (String url : urls) {
                String loc = manager.getResourceLocation(url, myProject);
                myPairs.add(new NameLocationPair(url, FileUtil.toSystemDependentName(loc), false));
            }
        }

        Collections.sort(myPairs);

        myIgnoredUrls = new ArrayList<>();
        final String[] ignoredResources = manager.getIgnoredResources();
        ContainerUtil.addAll(myIgnoredUrls, ignoredResources);

        Collections.sort(myIgnoredUrls);

        if (myExtPanel != null) {
            myExtPanel.setData(myPairs);
            myIgnorePanel.setData(myIgnoredUrls);
            if (!myNewPairs.isEmpty()) {
                ListSelectionModel selectionModel = myExtPanel.getTable().getSelectionModel();
                selectionModel.clearSelection();
                for (NameLocationPair newPair : myNewPairs) {
                    int index = myPairs.indexOf(newPair);
                    selectionModel.addSelectionInterval(index, index);
                }
            }
        }

        if (myProject != null) {
            myDefaultHtmlDoctype = manager.getDefaultHtmlDoctype(myProject);
            myHtmlLanguageLevelForm.resetFromDoctype(myDefaultHtmlDoctype);
        }

        setModified(!myNewPairs.isEmpty());
    }

    @Override
    public void disposeUIResources() {
        myPanel = null;
        myExtPanel = null;
        myIgnorePanel = null;
        myHtmlLanguageLevelForm = null;
    }

    @Override
    public String getHelpTopic() {
        return "preferences.externalResources";
    }

    @Nullable
    private NameLocationPair addExtLocation() {
        MapExternalResourceDialog dialog = new MapExternalResourceDialog(null, myProject, null, null);
        dialog.show();
        if (!dialog.isOK()) {
            return null;
        }
        setModified(true);
        return new NameLocationPair(dialog.getUri(), dialog.getResourceLocation(), false);
    }

    @Nullable
    private NameLocationPair editExtLocation(Object o) {
        NameLocationPair pair = (NameLocationPair) o;
        MapExternalResourceDialog dialog = new MapExternalResourceDialog(pair.getName(), myProject, null, pair.getLocation());
        dialog.show();
        if (!dialog.isOK()) {
            return null;
        }
        setModified(true);
        return new NameLocationPair(dialog.getUri(), dialog.getResourceLocation(), pair.myShared);
    }

    @Nullable
    private String addIgnoreLocation() {
        EditLocationDialog dialog = new EditLocationDialog(null, false);
        dialog.show();
        if (!dialog.isOK()) {
            return null;
        }
        setModified(true);
        return dialog.getPair().myName;
    }

    @Nullable
    private String editIgnoreLocation(Object o) {
        EditLocationDialog dialog = new EditLocationDialog(null, false);
        dialog.init(new NameLocationPair(o.toString(), null, false));
        dialog.show();
        if (!dialog.isOK()) {
            return null;
        }
        setModified(true);
        return dialog.getPair().myName;
    }

    private static class PathRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value != null) {
                String loc = value.toString().replace('\\', '/');
                final int jarDelimIndex = loc.indexOf(ArchiveFileSystem.ARCHIVE_SEPARATOR);
                VirtualFile path = null;

                if (jarDelimIndex != -1) {
                    final String protocol = VirtualFileManager.extractProtocol(loc);
                    final VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol);

                    if (fileSystem instanceof ArchiveFileSystem) {
                        path = fileSystem.findFileByPath(loc);
                    }
                }
                else {
                    path = LocalFileSystem.getInstance().findFileByPath(loc);
                }

                if (path != null) {
                    setForeground(UIUtil.getTableForeground(isSelected));
                }
                else {
                    setForeground(JBColor.RED);
                }
            }
            return rendererComponent;
        }
    }

    private static class IgnoredUrlsModel extends AddEditRemovePanel.TableModel<String> {
        private final String[] myNames = {XmlBundle.message("column.name.edit.external.resource.uri")};

        @Override
        public int getColumnCount() {
            return myNames.length;
        }

        @Override
        public Object getField(String o, int columnIndex) {
            return o;
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isEditable(int column) {
            return false;
        }

        @Override
        public void setValue(Object aValue, String data, int columnIndex) {

        }

        @Override
        public String getColumnName(int column) {
            return myNames[column];
        }
    }

    private class ExtUrlsTableModel extends AddEditRemovePanel.TableModel<NameLocationPair> {
        final String[] myNames;

        {
            List<String> names = new ArrayList<>();
            names.add(XmlBundle.message("column.name.edit.external.resource.uri"));
            names.add(XmlBundle.message("column.name.edit.external.resource.location"));
            if (myProject != null) {
                names.add("Project");
            }
            myNames = ArrayUtil.toStringArray(names);
        }

        @Override
        public int getColumnCount() {
            return myNames.length;
        }

        @Override
        public Object getField(NameLocationPair pair, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return pair.myName;
                case 1:
                    return pair.myLocation;
                case 2:
                    return !pair.myShared;
            }

            return "";
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return columnIndex == 2 ? Boolean.class : String.class;
        }

        @Override
        public boolean isEditable(int column) {
            return column == 2;
        }

        @Override
        public void setValue(Object aValue, NameLocationPair data, int columnIndex) {
            data.myShared = !((Boolean) aValue).booleanValue();
        }

        @Override
        public String getColumnName(int column) {
            return myNames[column];
        }
    }
}
