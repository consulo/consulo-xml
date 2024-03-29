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
 * Created by IntelliJ IDEA.
 * User: Maxim.Mossienko
 * Date: Oct 18, 2006
 * Time: 3:50:51 PM
 */
package consulo.xml.psi.impl.source.xml;

import consulo.language.impl.ast.Factory;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiNamedElement;
import consulo.language.util.IncorrectOperationException;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTokenType;

public class XmlElementChangeUtil {
  private XmlElementChangeUtil() {}

  static void doNameReplacement(final PsiNamedElement xmlElementDecl, XmlElement nameElement, final String name) throws
                                                                                                                 IncorrectOperationException {
    if (xmlElementDecl.isWritable() && isInProjectContent(xmlElementDecl.getProject(), xmlElementDecl.getContainingFile().getVirtualFile())) {

      if (nameElement!=null) {
        nameElement.replace(
          SourceTreeToPsiMap.treeElementToPsi(Factory.createSingleLeafElement(XmlTokenType.XML_NAME, name, null, xmlElementDecl.getManager()))
        );
      }
    }
  }

  static boolean isInProjectContent(Project project, VirtualFile vfile) {
    return vfile== null || ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(vfile)!=null;
  }
}
