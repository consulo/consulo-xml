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
package consulo.xml.lang.xml;

import consulo.ide.impl.psi.formatter.WhiteSpaceFormattingStrategyAdapter;
import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.*;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.util.CharTable;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlText;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 12/6/11 4:51 PM
 */
public class XmlWhiteSpaceFormattingStrategy extends WhiteSpaceFormattingStrategyAdapter {

  private static void addWhitespaceToTagBody(final ASTNode treePrev, final LeafElement whiteSpaceElement) {
    final CharTable charTable = SharedImplUtil.findCharTableByTree(treePrev);
    final ASTNode treeParent = treePrev.getTreeParent();

    final boolean before;
    final XmlText xmlText;
    if(treePrev.getElementType() == XmlElementType.XML_TEXT) {
      xmlText = (XmlText)treePrev.getPsi();
      before = true;
    }
    else if(treePrev.getTreePrev().getElementType() == XmlElementType.XML_TEXT){
      xmlText = (XmlText)treePrev.getTreePrev().getPsi();
      before = false;
    }
    else{
      xmlText = (XmlText)Factory.createCompositeElement(XmlElementType.XML_TEXT, charTable, treeParent.getPsi().getManager());
      CodeEditUtil.setNodeGenerated(xmlText.getNode(), true);
      treeParent.addChild(xmlText.getNode(), treePrev);
      before = true;
    }
    final ASTNode node = xmlText.getNode();
    assert node != null;
    final TreeElement anchorInText = (TreeElement) (before ? node.getFirstChildNode() : node.getLastChildNode());
    if (anchorInText == null) node.addChild(whiteSpaceElement);
    else if (anchorInText.getElementType() != XmlTokenType.XML_WHITE_SPACE) node.addChild(whiteSpaceElement, before ? anchorInText : null);
    else {
      final String text = before ? whiteSpaceElement.getText() + anchorInText.getText() : anchorInText.getText() +
                                                                                          whiteSpaceElement.getText();
      node.replaceChild(anchorInText, ASTFactory.whitespace(text));
    }
  }

  protected boolean isInsideTagBody(@Nonnull ASTNode place) {
    final ASTNode treeParent = place.getTreeParent();
    if(treeParent.getElementType() != XmlElementType.XML_TAG
       && treeParent.getElementType() != XmlElementType.HTML_TAG) return false;
    while(place != null){
      if(place.getElementType() == XmlTokenType.XML_TAG_END) return true;
      place = place.getTreePrev();
    }
    return false;
  }

  public boolean addWhitespace(@Nonnull final ASTNode treePrev, @Nonnull final LeafElement whiteSpaceElement) {
    if (isInsideTagBody(treePrev)) {
      addWhitespaceToTagBody(treePrev, whiteSpaceElement);
      return true;
    }

    return false;
  }

  public boolean containsWhitespacesOnly(@Nonnull final ASTNode node) {
    return (node.getElementType() == XmlTokenType.XML_DATA_CHARACTERS) &&
           node.getText().trim().length() == 0;
  }
}
