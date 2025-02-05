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

import consulo.proxy.EventDispatcher;
import consulo.util.lang.Comparing;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.xml.util.xml.DomElement;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.project.Project;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;

/**
 * @author peter
 */
public abstract class BaseControl<Bound extends JComponent, T> extends DomUIControl implements Highlightable {
  public static final Color ERROR_BACKGROUND = new Color(255,204,204);
  public static final Color ERROR_FOREGROUND = SimpleTextAttributes.ERROR_ATTRIBUTES.getFgColor();
  public static final Color WARNING_BACKGROUND = new Color(255,255,204);

  private final EventDispatcher<CommitListener> myDispatcher = EventDispatcher.create(CommitListener.class);

  private Bound myBoundComponent;
  private final DomWrapper<T> myDomWrapper;
  private boolean myCommitting;

  private Color myDefaultForeground;
  private Color myDefaultBackground;

  protected BaseControl(final DomWrapper<T> domWrapper) {
    myDomWrapper = domWrapper;
  }

  private void checkInitialized() {
    if (myBoundComponent != null) return;

    initialize(null);
  }

  protected JComponent getHighlightedComponent(Bound component) {
    return component;
  }

  protected final Color getDefaultBackground() {
    return myDefaultBackground;
  }

  protected final Color getDefaultForeground() {
    return myDefaultForeground;
  }

  protected final Color getErrorBackground() {
    return ERROR_BACKGROUND;
  }

  protected final Color getWarningBackground() {
    return WARNING_BACKGROUND;
  }

  protected final Color getErrorForeground() {
    return ERROR_FOREGROUND;
  }

  private void initialize(final Bound boundComponent) {
    myBoundComponent = createMainComponent(boundComponent);
    final JComponent highlightedComponent = getHighlightedComponent(myBoundComponent);
    myDefaultForeground = highlightedComponent.getForeground();
    myDefaultBackground = highlightedComponent.getBackground();
    final JComponent component = getComponentToListenFocusLost(myBoundComponent);
    if (component != null) {
      component.addFocusListener(new FocusListener() {
        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
          if (!e.isTemporary() && isValid()) {
            commit();
          }
        }
      });
    }

    updateComponent();
  }

  @Nullable
  protected JComponent getComponentToListenFocusLost(Bound component) {
    return null;
  }

  protected abstract Bound createMainComponent(Bound boundedComponent);

  public void bind(JComponent component) {
    initialize((Bound)component);
  }

  public void addCommitListener(CommitListener listener) {
    myDispatcher.addListener(listener);
  }

  public void removeCommitListener(CommitListener listener) {
    myDispatcher.removeListener(listener);
  }

  public final DomElement getDomElement() {
    return myDomWrapper.getWrappedElement();
  }

  public final DomWrapper<T> getDomWrapper() {
    return myDomWrapper;
  }

  public final Bound getComponent() {
    checkInitialized();
    return myBoundComponent;
  }

  public void dispose() {
  }

  public final void commit() {
    if (isValid() && !isCommitted()) {
      setValueToXml(getValue());
      updateComponent();
    }
  }

  protected final boolean isValid() {
    return myDomWrapper.isValid();
  }

  private static boolean valuesAreEqual(final Object valueInXml, final Object valueInControl) {
    return "".equals(valueInControl) && null == valueInXml ||
           equalModuloTrim(valueInXml, valueInControl) ||
           Comparing.equal(valueInXml, valueInControl);
  }

  private static boolean equalModuloTrim(final Object valueInXml, final Object valueInControl) {
    return valueInXml instanceof String && valueInControl instanceof String && ((String)valueInXml).trim().equals(((String)valueInControl).trim());
  }

  public final void reset() {
    if (!myCommitting) {
      doReset();
      updateComponent();
    }
  }

  public void updateHighlighting() {
    updateComponent();
  }

  protected void updateComponent() {
  }

  protected void doReset() {
    if (valuesDiffer()) {
      setValue(getValueFromXml());
    }
  }

  protected boolean isCommitted() {
    return !valuesDiffer();
  }

  private boolean valuesDiffer() {
    return !valuesAreEqual(getValueFromXml(), getValue());
  }

  private void setValueToXml(final T value) {
    if (myCommitting) return;
    myCommitting = true;
    try {
      final CommitListener multicaster = myDispatcher.getMulticaster();
      multicaster.beforeCommit(this);
      new WriteCommandAction(getProject(), getDomWrapper().getFile()) {
        protected void run(Result result) throws Throwable {
          doCommit(value);
        }
      }.execute();
      multicaster.afterCommit(this);
    }
    finally {
      myCommitting = false;
    }
  }

  protected void doCommit(final T value) throws IllegalAccessException, InvocationTargetException {
    myDomWrapper.setValue("".equals(value) ? null : value);
  }

  public final Project getProject() {
    return myDomWrapper.getProject();
  }

  private T getValueFromXml() {
    try {
      return myDomWrapper.getValue();
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean canNavigate(DomElement element) {
    return false;
  }

  public void navigate(DomElement element) {
  }

  @Nullable
  public abstract T getValue();

  public abstract void setValue(T value);
}
