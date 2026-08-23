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
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ex.awt.MnemonicHelper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.dom.*;
import consulo.xml.dom.editor.DomElementAnnotationsManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class DomFileEditor<T extends BasicDomElementComponent> extends PerspectiveFileEditor implements CommittablePanel, Highlightable {
    private final String myName;
    private final Supplier<? extends T> myComponentFactory;
    private T myComponent;

    public DomFileEditor(final DomElement element, final String name, final T component) {
        this(element.getManager().getProject(), DomUtil.getFile(element).getVirtualFile(), name, component);
    }

    public DomFileEditor(final Project project, final VirtualFile file, final String name, final T component) {
        this(project, file, name, (Supplier<T>) () -> component);
    }

    public DomFileEditor(final Project project, final VirtualFile file, final String name, final Supplier<? extends T> component) {
        super(project, file);
        myComponentFactory = component;
        myName = name;

        DomElementAnnotationsManager.getInstance(project).addHighlightingListener(element -> {
            if (isInitialised() && getComponent().isShowing() && element.isValid()) {
                updateHighlighting();
            }
        }, this);
    }

    @Override
    public void updateHighlighting() {
        if (checkIsValid()) {
            CommittableUtil.updateHighlighting(myComponent);
        }
    }

    @Override
    public void commit() {
        if (checkIsValid() && isInitialised()) {
            setShowing(false);
            try {
                getProject().getInstance(CommittableUtil.class).commit(myComponent);
            }
            finally {
                setShowing(true);
            }
        }
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        ensureInitialized();
        return myComponent.getComponent();
    }

    protected final T getDomComponent() {
        return myComponent;
    }

    @Override
    protected JComponent createCustomComponent() {
        MnemonicHelper.init(getComponent());
        myComponent = myComponentFactory.get();
        DomUIFactory.getDomUIFactory().setupErrorOutdatingUserActivityWatcher(this, getDomElement());
        DomManager.getDomManager(getProject()).addDomEventListener(new DomEventListener() {
            @Override
            public void eventOccured(DomEvent event) {
                checkIsValid();
            }
        }, this);
        Disposer.register(this, myComponent);
        return myComponent.getComponent();
    }

    @Override
    public final String getName() {
        return myName;
    }

    @Override
    protected DomElement getSelectedDomElement() {
        return DomUINavigationProvider.findDomElement(myComponent);
    }

    @Override
    protected void setSelectedDomElement(DomElement domElement) {
        final DomUIControl domControl = DomUINavigationProvider.findDomControl(myComponent, domElement);
        if (domControl != null) {
            domControl.navigate(domElement);
        }
    }

    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        ensureInitialized();
        return DomUIFactory.getDomUIFactory().createDomHighlighter(getProject(), this, getDomElement());
    }

    private DomElement getDomElement() {
        return myComponent.getDomElement();
    }


    @Override
    public boolean isValid() {
        return super.isValid() && (!isInitialised() || getDomElement().isValid());
    }

    @Override
    public void reset() {
        if (checkIsValid()) {
            myComponent.reset();
        }
    }
}
