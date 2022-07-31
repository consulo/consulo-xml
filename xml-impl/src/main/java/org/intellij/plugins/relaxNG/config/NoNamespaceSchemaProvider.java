/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.config;

import com.intellij.xml.XmlSchemaProvider;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
* Created by IntelliJ IDEA.
* User: sweinreuter
* Date: 22.11.2007
*/
@ExtensionImpl
public class NoNamespaceSchemaProvider extends XmlSchemaProvider {
  @Override
  @Nullable
  public XmlFile getSchema(@Nonnull @NonNls String url, @Nullable Module module, @Nonnull PsiFile baseFile) {
    if ("".equals(url)) {
      final Project project = baseFile.getProject();
      final VirtualFile file = NoNamespaceConfig.getInstance(project).getMappedFile(baseFile);
      if (file == null) return null;
      final PsiFile f = PsiManager.getInstance(project).findFile(file);
      if (f instanceof XmlFile) {
        return (XmlFile)f;
      }
    }
    return null;
  }

  @Override
  public boolean isAvailable(@Nonnull XmlFile file) {
    if (file.getFileType() != XmlFileType.INSTANCE) {
      return false;
    }
    NoNamespaceConfig config = NoNamespaceConfig.getInstance(file.getProject());
    return config != null && config.getMappedFile(file) != null;
  }
}