/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.intellij.plugins.relaxNG.model.resolve;

import consulo.ide.impl.idea.util.NullableFunction;
import consulo.language.psi.PsiManager;
import consulo.util.collection.ContainerUtil;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;
import javax.annotation.Nonnull;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.include.FileIncludeManager;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomManager;

/*
* Created by IntelliJ IDEA.
* User: sweinreuter
* Date: 09.06.2010
*/
public class RelaxIncludeIndex {
  public static boolean processForwardDependencies(XmlFile file, final PsiElementProcessor<XmlFile> processor) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return processor.execute(file);
    }
    final Project project = file.getProject();
    final VirtualFile[] files = FileIncludeManager.getManager(project).getIncludedFiles(virtualFile, true);

    return processRelatedFiles(file, files, processor);
  }

  public static boolean processBackwardDependencies(@Nonnull XmlFile file, PsiElementProcessor<XmlFile> processor) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return processor.execute(file);
    }
    final Project project = file.getProject();
    final VirtualFile[] files = FileIncludeManager.getManager(project).getIncludingFiles(virtualFile, true);

    return processRelatedFiles(file, files, processor);
  }

  private static boolean processRelatedFiles(PsiFile file, VirtualFile[] files, PsiElementProcessor<XmlFile> processor) {
    Project project = file.getProject();
    final PsiManager psiManager = PsiManager.getInstance(project);
    final PsiFile[] psiFiles = ContainerUtil.map2Array(files, PsiFile.class, (NullableFunction<VirtualFile, PsiFile>) file1 -> psiManager.findFile(file1));

    for (final PsiFile psiFile : psiFiles) {
      if (!processFile(psiFile, processor)) {
        return false;
      }
    }
    return true;
  }

  private static boolean processFile(PsiFile psiFile, PsiElementProcessor<XmlFile> processor) {
    final FileType type = psiFile.getFileType();
    if (type == XmlFileType.INSTANCE && isRngFile(psiFile)) {
      if (!processor.execute((XmlFile)psiFile)) {
        return false;
      }
    } else if (type == RncFileType.getInstance()) {
      if (!processor.execute((XmlFile)psiFile)) {
        return false;
      }
    }
    return true;
  }

  static boolean isRngFile(PsiFile psiFile) {
     return psiFile instanceof XmlFile && DomManager.getDomManager(psiFile.getProject()).getFileElement((XmlFile)psiFile, RngGrammar.class) != null;
  }
}
