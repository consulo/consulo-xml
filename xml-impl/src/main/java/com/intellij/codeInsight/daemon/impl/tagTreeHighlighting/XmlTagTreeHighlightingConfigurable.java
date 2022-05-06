/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl.tagTreeHighlighting;

import com.intellij.application.options.editor.XmlEditorOptions;
import consulo.codeEditor.Editor;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.idea.ui.breadcrumbs.BreadcrumbsWrapper;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Eugene.Kudelevsky
 */
public class XmlTagTreeHighlightingConfigurable implements UnnamedConfigurable {
  private JCheckBox myEnableTagTreeHighlightingCheckBox;
  private JSpinner myLevelsSpinner;
  private JPanel myLevelsPanel;
  private JPanel myContentPanel;
  private JSpinner myOpacitySpinner;

  public XmlTagTreeHighlightingConfigurable() {
    myLevelsSpinner.setModel(new SpinnerNumberModel(1, 1, 50, 1));
    myOpacitySpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.05));

    myEnableTagTreeHighlightingCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final boolean enabled = myEnableTagTreeHighlightingCheckBox.isSelected();
        UIUtil.setEnabled(myLevelsPanel, enabled, true);
      }
    });
  }

  @Override
  public JComponent createComponent() {
    return myContentPanel;
  }

  @Override
  public boolean isModified() {
    final XmlEditorOptions options = XmlEditorOptions.getInstance();

    if (myEnableTagTreeHighlightingCheckBox.isSelected() != options.isTagTreeHighlightingEnabled()) {
      return true;
    }

    if (getLevelCount() != options.getTagTreeHighlightingLevelCount()) {
      return true;
    }

    if (getOpacity() != options.getTagTreeHighlightingOpacity()) {
      return true;
    }

    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    final XmlEditorOptions options = XmlEditorOptions.getInstance();

    options.setTagTreeHighlightingEnabled(myEnableTagTreeHighlightingCheckBox.isSelected());
    options.setTagTreeHighlightingLevelCount(getLevelCount());
    options.setTagTreeHighlightingOpacity(getOpacity());

    clearTagTreeHighlighting();
  }

  private int getLevelCount() {
    return ((Integer)myLevelsSpinner.getValue()).intValue();
  }

  private int getOpacity() {
    return (int)(((Double)myOpacitySpinner.getValue()).doubleValue() * 100);
  }

  private static void clearTagTreeHighlighting() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      for (FileEditor fileEditor : FileEditorManager.getInstance(project).getAllEditors()) {
        if (fileEditor instanceof TextEditor) {
          final Editor editor = ((TextEditor)fileEditor).getEditor();
          XmlTagTreeHighlightingPass.clearHighlightingAndLineMarkers(editor, project);

          final BreadcrumbsWrapper breadcrumbsXmlWrapper = BreadcrumbsWrapper.getBreadcrumbsComponent(editor);
          if (breadcrumbsXmlWrapper != null) {
            breadcrumbsXmlWrapper.queueUpdate();
          }
        }
      }
    }
  }

  @Override
  public void reset() {
    final XmlEditorOptions options = XmlEditorOptions.getInstance();
    final boolean enabled = options.isTagTreeHighlightingEnabled();

    myEnableTagTreeHighlightingCheckBox.setSelected(enabled);
    myLevelsSpinner.setValue(options.getTagTreeHighlightingLevelCount());
    myOpacitySpinner.setValue(options.getTagTreeHighlightingOpacity() * 0.01);
    UIUtil.setEnabled(myLevelsPanel, enabled, true);
  }

  @Override
  public void disposeUIResources() {
  }
}
