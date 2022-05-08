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

import consulo.disposer.Disposer;
import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.util.Factory;
import consulo.project.Project;
import consulo.ui.ex.awt.MnemonicHelper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.events.DomEvent;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author peter
 */
public class DomFileEditor<T extends BasicDomElementComponent> extends PerspectiveFileEditor implements CommittablePanel, Highlightable {
  private final String myName;
  private final Factory<? extends T> myComponentFactory;
  private T myComponent;

  public DomFileEditor(final DomElement element, final String name, final T component) {
    this(element.getManager().getProject(), DomUtil.getFile(element).getVirtualFile(), name, component);
  }

  public DomFileEditor(final Project project, final VirtualFile file, final String name, final T component) {
    this(project, file, name, new Factory<T>() {
      public T create() {
        return component;
      }
    });
  }

  public DomFileEditor(final Project project, final VirtualFile file, final String name, final Factory<? extends T> component) {
    super(project, file);
    myComponentFactory = component;
    myName = name;

    DomElementAnnotationsManager.getInstance(project).addHighlightingListener(new DomElementAnnotationsManager.DomHighlightingListener() {
      public void highlightingFinished(@Nonnull DomFileElement element) {
        if (isInitialised() && getComponent().isShowing() && element.isValid()) {
          updateHighlighting();
        }
      }
    }, this);
  }

  public void updateHighlighting() {
    if (checkIsValid()) {
      CommittableUtil.updateHighlighting(myComponent);
    }
  }

  public void commit() {
    if (checkIsValid() && isInitialised()) {
      setShowing(false);
      try {
        ServiceManager.getService(getProject(), CommittableUtil.class).commit(myComponent);
      } finally {
        setShowing(true);
      }
    }
  }

  @Nullable
  public JComponent getPreferredFocusedComponent() {
    ensureInitialized();
    return myComponent.getComponent();
  }

  protected final T getDomComponent() {
    return myComponent;
  }

  @Nonnull
  protected JComponent createCustomComponent() {
    MnemonicHelper.init(getComponent());
    myComponent = myComponentFactory.create();
    DomUIFactory.getDomUIFactory().setupErrorOutdatingUserActivityWatcher(this, getDomElement());
    DomManager.getDomManager(getProject()).addDomEventListener(new DomEventListener() {
      public void eventOccured(DomEvent event) {
        checkIsValid();
      }
    }, this);
    Disposer.register(this, myComponent);
    return myComponent.getComponent();
  }

  @Nonnull
  public final String getName() {
    return myName;
  }

  protected DomElement getSelectedDomElement() {
    return DomUINavigationProvider.findDomElement(myComponent);
  }

  protected void setSelectedDomElement(DomElement domElement) {
    final DomUIControl domControl = DomUINavigationProvider.findDomControl(myComponent, domElement);
    if (domControl != null) {
      domControl.navigate(domElement);
    }
  }

  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    ensureInitialized();
    return DomUIFactory.getDomUIFactory().createDomHighlighter(getProject(), this, getDomElement());
  }

  private DomElement getDomElement() {
    return myComponent.getDomElement();
  }


  public boolean isValid() {
    return super.isValid() && (!isInitialised() || getDomElement().isValid());
  }

  public void reset() {
    if (checkIsValid()) {
      myComponent.reset();
    }
  }

  public static DomFileEditor createDomFileEditor(final String name,
                                                  @Nullable final Icon icon,
                                                  final DomElement element,
                                                  final Factory<? extends CommittablePanel> committablePanel) {

    final XmlFile file = DomUtil.getFile(element);
    final Factory<BasicDomElementComponent> factory = new Factory<BasicDomElementComponent>() {
      public BasicDomElementComponent create() {

        CaptionComponent captionComponent = new CaptionComponent(name, icon);
        captionComponent.initErrorPanel(element);
        BasicDomElementComponent component = createComponentWithCaption(committablePanel.create(), captionComponent, element);
        Disposer.register(component, captionComponent);
        return component;
      }
    };
    return new DomFileEditor<BasicDomElementComponent>(file.getProject(), file.getVirtualFile(), name, factory) {
      public JComponent getPreferredFocusedComponent() {
        return null;
      }
    };
  }

  public static BasicDomElementComponent createComponentWithCaption(final CommittablePanel committablePanel,
                                                                    final CaptionComponent captionComponent,
                                                                    final DomElement element) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(captionComponent, BorderLayout.NORTH);
    panel.add(element.isValid() ? committablePanel.getComponent() : new JPanel(), BorderLayout.CENTER);

    BasicDomElementComponent component = new BasicDomElementComponent(element) {
      public JComponent getComponent() {
        return panel;
      }
    };

    component.addComponent(committablePanel);
    component.addComponent(captionComponent);
    return component;
  }

}
