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

import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.*;
import consulo.language.Language;
import consulo.util.lang.Pair;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;

import java.util.HashMap;
import java.util.Map;

public abstract class XmlFormattingPolicy {

  private Map<Pair<PsiElement, Language>, Block> myRootToBlockMap = new HashMap<Pair<PsiElement, Language>, Block>();
  private boolean myProcessJsp = true;
  protected final FormattingDocumentModel myDocumentModel;
  private boolean myProcessJavaTree = true;

  protected XmlFormattingPolicy(final FormattingDocumentModel documentModel) {
    myDocumentModel = documentModel;
  }

  public void copyFrom(final XmlFormattingPolicy xmlFormattingPolicy) {
    myProcessJsp = xmlFormattingPolicy.myProcessJsp;
    myRootToBlockMap.putAll(xmlFormattingPolicy.myRootToBlockMap);
    myProcessJavaTree = xmlFormattingPolicy.processJavaTree(); 
  }

  public Block getOrCreateBlockFor(Pair<PsiElement, Language> root){
    if (!myRootToBlockMap.containsKey(root)) {
      myRootToBlockMap.put(root, createBlockFor(root));
    }
    return myRootToBlockMap.get(root);
  }

  private Block createBlockFor(final Pair<PsiElement,Language> root) {
    final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(root.getSecond(), root.getFirst());
    if (builder != null) {
      final Block result = builder.createModel(root.getFirst(), getSettings()).getRootBlock();
      if (result instanceof XmlBlock) {
        final XmlFormattingPolicy policy = getPolicy((XmlBlock)result);
        policy.setRootModels(myRootToBlockMap);
        policy.doNotProcessJsp();
      }
      return result;
    } else {
      return null;
    }
  }

  protected XmlFormattingPolicy getPolicy(final XmlBlock result) {
    return result.getPolicy();
  }

  private void doNotProcessJsp() {
    myProcessJsp = false;
  }

  private void setRootModels(final Map<Pair<PsiElement, Language>, Block> rootToBlockMap) {
    myRootToBlockMap = rootToBlockMap;
  }

  public abstract WrapType getWrappingTypeForTagEnd(XmlTag xmlTag);

  public abstract WrapType getWrappingTypeForTagBegin(final XmlTag tag);

  public abstract boolean insertLineBreakBeforeTag(XmlTag xmlTag);

  public abstract boolean insertLineBreakBeforeFirstAttribute(XmlAttribute attribute);

  public abstract boolean insertLineBreakAfterTagBegin(XmlTag tag);

  public abstract boolean removeLineBreakBeforeTag(XmlTag xmlTag);

  public abstract boolean keepWhiteSpacesInsideTag(XmlTag tag);

  public abstract boolean indentChildrenOf(XmlTag parentTag);

  public abstract boolean isTextElement(XmlTag tag);

  public abstract int getTextWrap(final XmlTag tag);

  public abstract int getAttributesWrap();

  public abstract boolean getShouldAlignAttributes();

  public abstract boolean getShouldAlignText();

  public abstract boolean getShouldKeepWhiteSpaces();

  public abstract boolean getShouldAddSpaceAroundEqualityInAttribute();

  public abstract boolean getShouldAddSpaceAroundTagName();

  public abstract int getKeepBlankLines();

  public abstract boolean getShouldKeepLineBreaks();

  public abstract boolean getShouldKeepLineBreaksInText();

  public abstract boolean getKeepWhiteSpacesInsideCDATA();

  public abstract int getWhiteSpaceAroundCDATAOption();

  public abstract CodeStyleSettings getSettings();

  public boolean processJsp() {
    return myProcessJsp;
  }

  public abstract boolean addSpaceIntoEmptyTag();

  public void setRootBlock(final ASTNode node, final Block rootBlock) {
    myRootToBlockMap.put(new Pair<PsiElement, Language>(node.getPsi(), node.getPsi().getLanguage()), rootBlock);
  }

  public FormattingDocumentModel getDocumentModel() {
    return myDocumentModel;
  }

  public abstract boolean shouldSaveSpacesBetweenTagAndText();

  public boolean processJavaTree() {
    return myProcessJavaTree;
  }


  public void dontProcessJavaTree() {
    myProcessJavaTree = false;
  }
}
