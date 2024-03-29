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

/*
 * @author max
 */
package consulo.xml.psi;

import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import java.util.List;

public class XmlRecursiveElementVisitor extends XmlElementVisitor {
  private final boolean myVisitAllFileRoots;

  public XmlRecursiveElementVisitor() {
    myVisitAllFileRoots = false;
  }

  public XmlRecursiveElementVisitor(final boolean visitAllFileRoots) {
    myVisitAllFileRoots = visitAllFileRoots;
  }

  public void visitElement(final PsiElement element) {
    element.acceptChildren(this);
  }

  @Override
  public void visitFile(final PsiFile file) {
    if (myVisitAllFileRoots) {
      final FileViewProvider viewProvider = file.getViewProvider();
      final List<PsiFile> allFiles = viewProvider.getAllFiles();
      if (allFiles.size() > 1) {
        if (file == viewProvider.getPsi(viewProvider.getBaseLanguage())) {
          for (PsiFile lFile : allFiles) {
            lFile.acceptChildren(this);
          }
          return;
        }
      }
    }

    super.visitFile(file);
  }
}