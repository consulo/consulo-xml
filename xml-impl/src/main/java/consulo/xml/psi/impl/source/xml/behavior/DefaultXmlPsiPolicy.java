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
package consulo.xml.psi.impl.source.xml.behavior;

import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.impl.psi.DummyHolderFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.util.CharTable;
import consulo.logging.Logger;
import consulo.xml.psi.impl.source.xml.XmlPsiPolicy;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlTagChild;

public abstract class DefaultXmlPsiPolicy implements XmlPsiPolicy {
  private static final Logger LOG = Logger.getInstance(DefaultXmlPsiPolicy.class);

  public ASTNode encodeXmlTextContents(String displayText, PsiElement text) {
    final PsiFile containingFile = text.getContainingFile();
    CharTable charTable = SharedImplUtil.findCharTableByTree(text.getNode());
    final FileElement dummyParent = DummyHolderFactory.createHolder(text.getManager(), null, charTable).getTreeElement();
    final XmlTag rootTag =
      ((XmlFile)PsiFileFactory.getInstance(containingFile.getProject())
        .createFileFromText("a.xml", "<a>" + displayText + "</a>")).getRootTag();

    assert rootTag != null;
    final XmlTagChild[] tagChildren = rootTag.getValue().getChildren();

    final XmlTagChild child = tagChildren.length > 0 ? tagChildren[0]:null;
    LOG.assertTrue(child != null, "Child is null for tag: " + rootTag.getText());

    final TreeElement element = (TreeElement)child.getNode();
    ((TreeElement)tagChildren[tagChildren.length - 1].getNode().getTreeNext()).rawRemoveUpToLast();
    dummyParent.rawAddChildren(element);
    TreeUtil.clearCaches(dummyParent);
    return element.getFirstChildNode();
  }

}
