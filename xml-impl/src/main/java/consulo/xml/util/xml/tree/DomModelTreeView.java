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

package consulo.xml.util.xml.tree;

import consulo.application.ApplicationManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.PlatformDataKeys;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.awt.tree.action.CollapseAllAction;
import consulo.ui.ex.awt.tree.action.ExpandAllAction;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class DomModelTreeView extends Wrapper implements DataProvider, Disposable {
  public static final Key<DomModelTreeView> DATA_KEY = Key.create("DOM_MODEL_TREE_VIEW_KEY");
  @NonNls
  public static String DOM_MODEL_TREE_VIEW_POPUP = "DOM_MODEL_TREE_VIEW_POPUP";

  private final SimpleTree myTree;
  private final AbstractTreeBuilder myBuilder;
  private DomManager myDomManager;
  @Nullable
  private DomElement myRootElement;

  public DomModelTreeView(@Nonnull DomElement rootElement) {
    this(rootElement, rootElement.getManager(), new DomModelTreeStructure(rootElement));
  }

  protected DomModelTreeView(DomElement rootElement, DomManager manager, SimpleTreeStructure treeStructure) {
    myDomManager = manager;
    myRootElement = rootElement;
    myTree = new SimpleTree(new DefaultTreeModel(new DefaultMutableTreeNode()));
    myTree.setRootVisible(isRootVisible());
    myTree.setShowsRootHandles(true);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    ToolTipManager.sharedInstance().registerComponent(myTree);
    TreeUtil.installActions(myTree);

    myBuilder = new AbstractTreeBuilder(myTree, (DefaultTreeModel) myTree.getModel(), treeStructure, WeightBasedComparator.INSTANCE, false);
    Disposer.register(this, myBuilder);

    myBuilder.setNodeDescriptorComparator(null);

    myBuilder.initRootNode();

    add(myTree, BorderLayout.CENTER);

    myTree.addTreeExpansionListener(new TreeExpansionListener() {
      public void treeExpanded(TreeExpansionEvent event) {
        final SimpleNode simpleNode = myTree.getNodeFor(event.getPath());

        if (simpleNode instanceof AbstractDomElementNode) {
          ((AbstractDomElementNode) simpleNode).setExpanded(true);
        }
      }

      public void treeCollapsed(TreeExpansionEvent event) {
        final SimpleNode simpleNode = myTree.getNodeFor(event.getPath());

        if (simpleNode instanceof AbstractDomElementNode) {
          ((AbstractDomElementNode) simpleNode).setExpanded(false);
          simpleNode.update();
        }
      }
    });

    myDomManager.addDomEventListener(new DomChangeAdapter() {
      protected void elementChanged(DomElement element) {
        if (element.isValid()) {
          queueUpdate(DomUtil.getFile(element).getVirtualFile());
        } else if (element instanceof DomFileElement) {
          final XmlFile xmlFile = ((DomFileElement) element).getFile();
          queueUpdate(xmlFile.getVirtualFile());
        }
      }
    }, this);


    final Project project = myDomManager.getProject();
    DomElementAnnotationsManager.getInstance(project).addHighlightingListener(new DomElementAnnotationsManager.DomHighlightingListener() {
      public void highlightingFinished(DomFileElement element) {
        if (element.isValid()) {
          queueUpdate(DomUtil.getFile(element).getVirtualFile());
        }
      }
    }, this);

    myTree.setPopupGroup(getPopupActions(), DOM_MODEL_TREE_VIEW_POPUP);
  }

  protected boolean isRightFile(final VirtualFile file) {
    return myRootElement == null || (myRootElement.isValid() && file.equals(DomUtil.getFile(myRootElement).getVirtualFile()));
  }

  private void queueUpdate(final VirtualFile file) {
    if (file == null) return;
    if (getProject().isDisposed()) return;
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        if (getProject().isDisposed()) return;
        if (!file.isValid() || isRightFile(file)) {
          myBuilder.updateFromRoot();
        }
      }
    });
  }

  protected boolean isRootVisible() {
    return true;
  }

  public final void updateTree() {
    myBuilder.updateFromRoot();
  }

  public DomElement getRootElement() {
    return myRootElement;
  }

  protected final Project getProject() {
    return myDomManager.getProject();
  }

  public AbstractTreeBuilder getBuilder() {
    return myBuilder;
  }

  public void dispose() {
  }

  public SimpleTree getTree() {
    return myTree;
  }

  protected ActionGroup getPopupActions() {
    DefaultActionGroup group = new DefaultActionGroup();

    group.add(ActionManager.getInstance().getAction("DomElementsTreeView.TreePopup"));
    group.addSeparator();

    group.add(new ExpandAllAction(myTree));
    group.add(new CollapseAllAction(myTree));

    return group;
  }

  @Override
  @Nullable
  public Object getData(@Nonnull Key<?> dataId) {
    if (DomModelTreeView.DATA_KEY == dataId) {
      return this;
    }
    final SimpleNode simpleNode = getTree().getSelectedNode();
    if (simpleNode instanceof AbstractDomElementNode) {
      final DomElement domElement = ((AbstractDomElementNode) simpleNode).getDomElement();
      if (domElement != null && domElement.isValid()) {
        if (PlatformDataKeys.NAVIGATABLE_ARRAY == dataId) {
          final XmlElement tag = domElement.getXmlElement();
          if (tag instanceof Navigatable) {
            return new Navigatable[]{(Navigatable) tag};
          }
        }
      }
    }
    return null;
  }

  public void setSelectedDomElement(final DomElement domElement) {
    if (domElement == null) return;

    final SimpleNode node = getNodeFor(domElement);

    if (node != null) {
      getTree().setSelectedNode(getBuilder(), node, true);
    }
  }

  @Nullable
  private SimpleNode getNodeFor(final DomElement domElement) {
    return visit((SimpleNode) myBuilder.getTreeStructure().getRootElement(), domElement);
  }

  @Nullable
  private SimpleNode visit(SimpleNode simpleNode, DomElement domElement) {
    boolean validCandidate = false;
    if (simpleNode instanceof AbstractDomElementNode) {
      final DomElement nodeElement = ((AbstractDomElementNode) simpleNode).getDomElement();
      if (nodeElement != null) {
        validCandidate = !(simpleNode instanceof DomElementsGroupNode);
        if (validCandidate && nodeElement.equals(domElement)) {
          return simpleNode;
        }
        if (!(nodeElement instanceof MergedObject) && !isParent(nodeElement, domElement)) {
          return null;
        }
      }
    }
    final Object[] childElements = myBuilder.getTreeStructure().getChildElements(simpleNode);
    if (childElements.length == 0 && validCandidate) { // leaf
      return simpleNode;
    }
    for (Object child : childElements) {
      SimpleNode result = visit((SimpleNode) child, domElement);
      if (result != null) {
        return result;
      }
    }
    return validCandidate ? simpleNode : null;
  }

  private static boolean isParent(final DomElement potentialParent, final DomElement domElement) {
    DomElement currParent = domElement;
    while (currParent != null) {
      if (currParent.equals(potentialParent)) return true;

      currParent = currParent.getParent();
    }
    return false;
  }

}

