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
package consulo.xml.refactoring.rename;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.usage.UsageInfo;
import consulo.util.collection.Queue;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "xmlAttribute")
public class RenameXmlAttributeProcessor extends RenamePsiElementProcessor {
  private static final Logger LOG = Logger.getInstance(RenameXmlAttributeProcessor.class);

  public boolean canProcessElement(@Nonnull final PsiElement element) {
    return element instanceof XmlAttribute || element instanceof XmlAttributeValue;
  }

  public void renameElement(final PsiElement element,
                            final String newName,
                            final UsageInfo[] usages,
                            @Nullable RefactoringElementListener listener) throws IncorrectOperationException {
    if (element instanceof XmlAttribute) {
      doRenameXmlAttribute((XmlAttribute)element, newName, listener);
    }
    else if (element instanceof XmlAttributeValue) {
      doRenameXmlAttributeValue((XmlAttributeValue)element, newName, usages, listener);
    }
  }

  private static void doRenameXmlAttribute(XmlAttribute attribute,
                                           String newName,
                                           @Nullable RefactoringElementListener listener) {
    try {
      final PsiElement element = attribute.setName(newName);
      if (listener != null) {
        listener.elementRenamed(element);
      }
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  private static void doRenameXmlAttributeValue(@Nonnull XmlAttributeValue value,
                                                String newName,
                                                UsageInfo[] infos,
                                                @Nullable RefactoringElementListener listener)
    throws consulo.language.util.IncorrectOperationException {
    LOG.assertTrue(value.isValid());

    renameAll(value, infos, newName, value.getValue());

    PsiManager psiManager = value.getManager();
    LOG.assertTrue(psiManager != null);
    XmlFile file = (XmlFile)PsiFileFactory.getInstance(psiManager.getProject()).createFileFromText("dummy.xml", "<a attr=\"" + newName + "\"/>");
    final PsiElement element = value.replace(file.getDocument().getRootTag().getAttributes()[0].getValueElement());
    if (listener != null) {
      listener.elementRenamed(element);
    }
  }

  private static void renameAll(PsiElement originalElement, UsageInfo[] infos, String newName,
                                String originalName) throws IncorrectOperationException {
    if (newName.equals(originalName)) return;
    Queue<PsiReference> queue = new Queue<PsiReference>(infos.length);
    for (UsageInfo info : infos) {
      if (info.getElement() == null) continue;
      PsiReference ref = info.getReference();
      if (ref == null) continue;
      queue.addLast(ref);
    }

    while(!queue.isEmpty()) {
      final PsiReference reference = queue.pullFirst();
      final PsiElement oldElement = reference.getElement();
      if (!oldElement.isValid() || oldElement == originalElement) continue;
      final PsiElement newElement = reference.handleElementRename(newName);
      if (!oldElement.isValid()) {
        for (PsiReference psiReference : ReferencesSearch.search(originalElement, new LocalSearchScope(newElement), false)) {
          queue.addLast(psiReference);
        }
      }
    }
  }

}
