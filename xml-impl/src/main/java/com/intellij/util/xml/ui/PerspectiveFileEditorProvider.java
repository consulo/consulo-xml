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

import javax.annotation.Nonnull;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorPolicy;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.disposer.Disposer;
import consulo.fileEditor.WeighedFileEditorProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

/**
 * @author peter
 */
public abstract class PerspectiveFileEditorProvider extends WeighedFileEditorProvider {
  @Nonnull
  public abstract PerspectiveFileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file);

  public void disposeEditor(@Nonnull FileEditor editor) {
    Disposer.dispose(editor);
  }

  @Nonnull
  public FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
    return new FileEditorState() {
      public boolean canBeMergedWith(FileEditorState otherState, FileEditorStateLevel level) {
        return true;
      }
    };
  }

  public void writeState(@Nonnull FileEditorState state, @Nonnull Project project, @Nonnull Element targetElement) {
  }

  @Nonnull
  @NonNls
  public final String getEditorTypeId() {
    return getComponentName();
  }

  @Nonnull
  public final FileEditorPolicy getPolicy() {
    return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
  }

  @NonNls
  public final String getComponentName() {
    return getClass().getName();
  }

}
