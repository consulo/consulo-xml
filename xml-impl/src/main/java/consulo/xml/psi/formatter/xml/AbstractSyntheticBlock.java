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
package consulo.xml.psi.formatter.xml;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.Alignment;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.Indent;
import consulo.language.codeStyle.Wrap;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.logging.Logger;
import consulo.language.codeStyle.AbstractBlock;
import consulo.language.psi.OuterLanguageElement;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.codeStyle.WrapType;

import java.util.List;


public abstract class AbstractSyntheticBlock implements Block {
  protected final Indent myIndent;
  protected final XmlFormattingPolicy myXmlFormattingPolicy;
  protected final ASTNode myEndTreeNode;
  protected final ASTNode myStartTreeNode;
  private final XmlTag myTag;

  public AbstractSyntheticBlock(List<Block> subBlocks, Block parent, XmlFormattingPolicy policy, Indent indent) {
    myEndTreeNode = getLastNode(subBlocks);
    myStartTreeNode = getFirstNode(subBlocks);
    myIndent = indent;
    myXmlFormattingPolicy = policy;
    if (parent instanceof AbstractXmlBlock) {
      myTag = ((AbstractXmlBlock)parent).getTag();
    }
    else if (parent instanceof AbstractSyntheticBlock) {
      myTag = ((AbstractSyntheticBlock)parent).getTag();
    } else {
      throw new IllegalStateException("Parent should be AbstractXmlBlock or AbstractSyntheticBlock, but it is " + parent.getClass());
    }

  }

  private static final Logger LOG = Logger.getInstance(AbstractSyntheticBlock.class);

  private ASTNode getFirstNode(final List<Block> subBlocks) {
    LOG.assertTrue(!subBlocks.isEmpty());
    final Block firstBlock = subBlocks.get(0);
    if (firstBlock instanceof AbstractBlock) {
      return ((AbstractBlock)firstBlock).getNode();
    }
    else {
      return getFirstNode(firstBlock.getSubBlocks());
    }
  }

  private ASTNode getLastNode(final List<Block> subBlocks) {
    LOG.assertTrue(!subBlocks.isEmpty());
    final Block lastBlock = subBlocks.get(subBlocks.size() - 1);
    if (lastBlock instanceof AbstractBlock) {
      return ((AbstractBlock)lastBlock).getNode();
    }
    else {
      return getLastNode(lastBlock.getSubBlocks());
    }
  }

  private boolean isEndOfTag() {
    return myEndTreeNode.getElementType() == XmlElementType.XML_TAG_END;
  }

  public Wrap getWrap() {
    return null;
  }

  public Indent getIndent() {
    return myIndent;
  }

  public Alignment getAlignment() {
    return null;
  }

  protected static boolean isXmlTagName(final IElementType type1, final IElementType type2) {
    if (type1 == XmlElementType.XML_NAME && type2 == XmlElementType.XML_TAG_END) return true;
    if (type1 == XmlElementType.XML_NAME && type2 == XmlElementType.XML_EMPTY_ELEMENT_END) return true;
    if (type1 == XmlElementType.XML_ATTRIBUTE && type2 == XmlElementType.XML_EMPTY_ELEMENT_END) return true;
    return type1 == XmlElementType.XML_ATTRIBUTE && type2 == XmlElementType.XML_TAG_END;
  }

  public boolean endsWithText() {
    return myEndTreeNode.getElementType() == XmlElementType.XML_TEXT ||
           myEndTreeNode.getElementType() == XmlElementType.XML_DATA_CHARACTERS;
  }

  public boolean isTagDescription() {
    final ASTNode startTreeNode = myStartTreeNode;
    return isTagDescription(startTreeNode);
  }

  private static boolean isTagDescription(final ASTNode startTreeNode) {
    return startTreeNode.getElementType() == XmlElementType.XML_START_TAG_START ||
           startTreeNode.getElementType() == XmlElementType.XML_END_TAG_START;
  }

  public boolean startsWithText() {
    return myStartTreeNode.getElementType() == XmlElementType.XML_TEXT ||
           myStartTreeNode.getElementType() == XmlElementType.XML_DATA_CHARACTERS;
  }

  public boolean endsWithTextElement() {
    if (endsWithText()) return true;
    if (isEndOfTag() && myXmlFormattingPolicy.isTextElement(getTag())) return true;
    return isTextTag(myEndTreeNode);
  }

  protected XmlTag getTag() {
    return myTag;
  }

  public boolean startsWithTextElement() {
    if (startsWithText()) return true;
    if (isStartOfTag() && myXmlFormattingPolicy.isTextElement(getTag())) return true;
    return isTextTag(myStartTreeNode);
  }

  private boolean isTextTag(final ASTNode treeNode) {
    return isXmlTag(treeNode) && myXmlFormattingPolicy.isTextElement((XmlTag)SourceTreeToPsiMap.treeElementToPsi(treeNode));
  }

  private boolean isXmlTag(final ASTNode treeNode) {
    return (treeNode.getPsi() instanceof XmlTag);
  }

  private boolean isStartOfTag() {
    return isTagDescription(myStartTreeNode);
  }

  protected static TextRange calculateTextRange(final List<Block> subBlocks) {
    return new TextRange(subBlocks.get(0).getTextRange().getStartOffset(),
                         subBlocks.get(subBlocks.size() - 1).getTextRange().getEndOffset());
  }

  public boolean isIncomplete() {
    return getSubBlocks().get(getSubBlocks().size() - 1).isIncomplete();
  }

  public boolean startsWithTag() {
    return isXmlTag(myStartTreeNode);
  }

  public XmlTag getStartTag() {
    return (XmlTag)myStartTreeNode.getPsi();
  }


  public boolean endsWithTag() {
    return isXmlTag(myEndTreeNode);
  }

  public boolean isJspTextBlock() {
    return false;
  }

  public boolean isJspxTextBlock() {
    return false;
  }

  /**
   * Checks if the block contains a single node which belongs to the outer (template) language.
   *
   * @return True if it does, False otherwise.
   */
  public boolean isOuterLanguageBlock() {
    return (myStartTreeNode == myEndTreeNode) && (myStartTreeNode instanceof OuterLanguageElement);
  }

  public boolean isLeaf() {
    return false;
  }

  public boolean startsWithCDATA() {
    return isCDATA(myStartTreeNode.getFirstChildNode());
  }

  private boolean isCDATA(final ASTNode node) {
    return node != null && node.getElementType() == XmlElementType.XML_CDATA;
  }

  public boolean containsCDATA() {
    return myStartTreeNode.getElementType() == XmlElementType.XML_CDATA_START &&
           myEndTreeNode.getElementType() == XmlElementType.XML_CDATA_END;
  }

  public boolean endsWithCDATA() {
    return isCDATA(myStartTreeNode.getLastChildNode());
  }

  public boolean insertLineFeedAfter() {
    final List<Block> subBlocks = getSubBlocks();
    final Block lastBlock = subBlocks.get(subBlocks.size() - 1);
    if (lastBlock instanceof XmlTagBlock) {
      return insertLineFeedAfter(((XmlTagBlock)lastBlock).getTag());
    }
    if (endsWithText()) {
      return insertLineFeedAfter(myTag);
    }
    return false;
  }

  protected boolean insertLineFeedAfter(final XmlTag tag) {
    return myXmlFormattingPolicy.getWrappingTypeForTagBegin(tag) == WrapType.ALWAYS;
  }

}
