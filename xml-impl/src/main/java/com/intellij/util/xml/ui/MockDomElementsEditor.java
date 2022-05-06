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
package com.intellij.util.xml.ui;

import consulo.application.ApplicationManager;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.module.Module;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.util.Factory;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.StableElement;
import consulo.project.Project;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.*;

/**
 * @author peter
 */
public class MockDomElementsEditor {
  private final Map<EditedElementDescription<? extends DomElement>, DomElement> myDomElements = new HashMap<EditedElementDescription<? extends DomElement>, DomElement>();
  private final consulo.module.Module myModule;
  private CommittablePanel myContents;
  private DomFileEditor myFileEditor;

  public MockDomElementsEditor(final consulo.module.Module module) {
    myModule = module;
  }

  protected final <T extends DomElement> T addEditedElement(final Class<T> aClass, final EditedElementDescription<T> description) {
    final DomManager domManager = DomManager.getDomManager(myModule.getProject());
    final T t = domManager.createStableValue(new Factory<T>() {
      public T create() {
        T t = description.find();
        if (t == null) {
          return createMockElement(aClass);
        }
        return t;
      }
    });
    myDomElements.put(description, t);
    return t;
  }

  protected <T extends DomElement> T createMockElement(final Class<T> aClass) {
    return createMockElement(aClass, myModule);
  }

  protected final DomFileEditor initFileEditor(final BasicDomElementComponent component, final VirtualFile virtualFile, final String name) {
    initFileEditor(component.getProject(), virtualFile, name, new Factory<BasicDomElementComponent>() {
      public BasicDomElementComponent create() {
        return component;
      }
    });
    Disposer.register(myFileEditor, component);
    return myFileEditor;
  }

  protected final DomFileEditor initFileEditor(final Project project, final VirtualFile virtualFile, final String name, final Factory<? extends BasicDomElementComponent> component) {
    myFileEditor = new DomFileEditor<BasicDomElementComponent>(project, virtualFile, name, component) {
      public JComponent getPreferredFocusedComponent() {
        return null;
      }

      @Nonnull
      protected JComponent createCustomComponent() {
        final JComponent customComponent = super.createCustomComponent();
        myContents = getDomComponent();
        return customComponent;
      }

      public void reset() {
        for (final Map.Entry<EditedElementDescription<? extends DomElement>, DomElement> entry : myDomElements.entrySet()) {
          final DomElement newValue = entry.getKey().find();
          final DomElement oldValue = entry.getValue();
          if (newValue != null && !newValue.equals(oldValue) || newValue == null && !oldValue.getManager().isMockElement(oldValue)) {
            ((StableElement)oldValue).revalidate();
          }
        }
        super.reset();
      }

      public void commit() {
        super.commit();
        final List<EditedElementDescription> descriptions = new ArrayList<EditedElementDescription>();
        final Set<PsiFile> changedFiles = new HashSet<PsiFile>();
        for (final Map.Entry<EditedElementDescription<? extends DomElement>, DomElement> entry : myDomElements.entrySet()) {
          final EditedElementDescription description = entry.getKey();
            final DomElement editedElement = entry.getValue();
            if (description.find() == null && editedElement.getXmlTag() != null) {
              descriptions.add(description);
              final XmlFile xmlFile = description.getEditedFile();
              if (xmlFile != null) {
                changedFiles.add(xmlFile);
              }
            }
        }
        new WriteCommandAction(project, PsiUtilCore.toPsiFileArray(changedFiles)) {
          protected void run(Result result) throws Throwable {
            for (EditedElementDescription description : descriptions) {
              final DomElement editedElement = myDomElements.get(description);
              DomElement element = description.addElement();
              element.copyFrom(editedElement);
              description.initialize(element);
              removeWatchedElement(editedElement);
              ((StableElement)editedElement).invalidate();
            }
          }
        }.execute();
      }
    };
    final DomManager domManager = DomManager.getDomManager(project);
    for (final DomElement element : myDomElements.values()) {
      if (domManager.isMockElement(element)) {
        myFileEditor.addWatchedElement(element);
      }
    }
    return myFileEditor;
  }

  public final DomFileEditor getFileEditor() {
    return myFileEditor;
  }

  private <T extends DomElement> T createMockElement(final Class<T> aClass, final Module module) {
    final Project project = module.getProject();
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        if (myFileEditor.isInitialised()) {
          myContents.reset();
        }
      }
    });
    final DomManager domManager = DomManager.getDomManager(project);
    final T mockElement = domManager.createMockElement(aClass, module, true);
    if (myFileEditor != null) {
      myFileEditor.addWatchedElement(mockElement);
    }
    return mockElement;
  }
}
