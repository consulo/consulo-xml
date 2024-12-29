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

package consulo.xml.lang;

import com.intellij.xml.util.XmlTagUtil;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressManager;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.document.util.UnfairTextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.psi.impl.source.html.HtmlFileImpl;
import consulo.xml.psi.xml.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class XmlCodeFoldingBuilder implements FoldingBuilder, DumbAware {
  private static final Logger LOG = Logger.getInstance(XmlCodeFoldingBuilder.class);
  private static final TokenSet XML_ATTRIBUTE_SET = TokenSet.create(XmlElementType.XML_ATTRIBUTE);
  private static final int MIN_TEXT_RANGE_LENGTH = 3;
  private static final String STYLE_ATTRIBUTE = "style";

  @Nonnull
  public FoldingDescriptor[] buildFoldRegions(@Nonnull ASTNode node, @Nonnull Document document) {
    final PsiElement psiElement = node.getPsi();
    XmlDocument xmlDocument = null;

    if (psiElement instanceof XmlFile) {
      XmlFile file = (XmlFile)psiElement;
      xmlDocument = file.getDocument();
    }
    else if (psiElement instanceof XmlDocument) {
      xmlDocument = (XmlDocument)psiElement;
    }

    XmlElement rootTag = xmlDocument == null ? null : xmlDocument.getRootTag();
    if (rootTag == null) {
      rootTag = xmlDocument;
    }
    List<FoldingDescriptor> foldings = null;

    if (rootTag != null) {
      foldings = new ArrayList<FoldingDescriptor>();

      doAddForChildren(xmlDocument, foldings, document);
    }

    return foldings != null ? foldings.toArray(new FoldingDescriptor[foldings.size()]): FoldingDescriptor.EMPTY;
  }

  protected void addElementsToFold(List<FoldingDescriptor> foldings, XmlElement tag, Document document) {
    addToFold(foldings, tag, document);
    doAddForChildren(tag, foldings, document);
  }

  protected void doAddForChildren(final XmlElement tag, final List<FoldingDescriptor> foldings, final Document document) {
    final PsiElement[] children = tag.getChildren();

    for (PsiElement child : children) {
      ProgressManager.checkCanceled();

      if (child instanceof XmlTag || child instanceof XmlConditionalSection) {
        addElementsToFold(foldings, (XmlElement)child, document);
      }
      else if (child instanceof XmlComment) {
        addToFold(foldings, child, document);
      }
      else if (child instanceof XmlText || child instanceof XmlProlog) {
        final PsiElement[] grandChildren = child.getChildren();

        for (PsiElement grandChild : grandChildren) {
          ProgressManager.checkCanceled();

          if (grandChild instanceof XmlComment) {
            addToFold(foldings, grandChild, document);
          }
        }
      }
      else if(child instanceof XmlAttribute && isAttributeShouldBeFolded((XmlAttribute)child)) {
        addToFold(foldings, child, document);
      }
      else {
        final Language language = child.getLanguage();
        if (!(language instanceof XMLLanguage) && language != Language.ANY) {
          final FoldingBuilder foldingBuilder = FoldingBuilder.forLanguageComposite(language);

          if (foldingBuilder != null) {
            final FoldingDescriptor[] foldingDescriptors = foldingBuilder.buildFoldRegions(child.getNode(), document);

            ContainerUtil.addAll(foldings, foldingDescriptors);
          }
        }
      }
    }
  }

  @Nullable
  public TextRange getRangeToFold(PsiElement element) {
    if (element instanceof XmlTag) {
      final ASTNode tagNode = element.getNode();
      XmlToken tagNameElement = XmlTagUtil.getStartTagNameElement((XmlTag)element);
      if (tagNameElement == null) return null;

      int nameEnd = tagNameElement.getTextRange().getEndOffset();
      int end = tagNode.getLastChildNode().getTextRange().getEndOffset() - 1;  // last child node can be another tag in unbalanced tree
      ASTNode[] attributes = tagNode.getChildren(XML_ATTRIBUTE_SET);

      if (attributes.length > 0) {
        ASTNode lastAttribute = attributes[attributes.length - 1];
        ASTNode lastAttributeBeforeCR = null;

        for (ASTNode child = tagNode.getFirstChildNode(); child != lastAttribute.getTreeNext(); child = child.getTreeNext()) {
          if (child.getElementType() == XmlElementType.XML_ATTRIBUTE) {
            lastAttributeBeforeCR = child;
          } else if (child.getPsi() instanceof PsiWhiteSpace) {
            if (child.textContains('\n')) break;
          }
        }

        if (lastAttributeBeforeCR != null) {
          int attributeEnd = lastAttributeBeforeCR.getTextRange().getEndOffset();
          return new UnfairTextRange(attributeEnd, end);
        }
      }

      return new UnfairTextRange(nameEnd, end);
    } else if (element instanceof XmlComment) {
      final XmlComment xmlComment = (XmlComment)element;
      final TextRange textRange = element.getTextRange();
      int commentStartOffset = getCommentStartOffset(xmlComment);
      int commentEndOffset = getCommentStartEnd(xmlComment);

      if (textRange.getEndOffset() - textRange.getStartOffset() > commentStartOffset + commentEndOffset) {
        return new TextRange(textRange.getStartOffset() + commentStartOffset, textRange.getEndOffset() - commentEndOffset);
      }
      else {
        return null;
      }
    }
    else if (element instanceof XmlConditionalSection) {
      final XmlConditionalSection conditionalSection = (XmlConditionalSection)element;
      final TextRange textRange = element.getTextRange();
      final PsiElement bodyStart = conditionalSection.getBodyStart();
      int startOffset = bodyStart != null ? bodyStart.getStartOffsetInParent() : MIN_TEXT_RANGE_LENGTH;
      int endOffset = MIN_TEXT_RANGE_LENGTH;

      if (textRange.getEndOffset() - textRange.getStartOffset() > startOffset + endOffset) {
        return new TextRange(textRange.getStartOffset() + startOffset, textRange.getEndOffset() - endOffset);
      }
      else {
        return null;
      }
    }
    else if (element instanceof XmlAttribute) {
      final XmlAttributeValue valueElement = ((XmlAttribute)element).getValueElement();
      return valueElement != null ? valueElement.getValueTextRange() : null;
    }
    else {
      return null;
    }
  }

  protected int getCommentStartOffset(final XmlComment element) {
    return 4;
  }

  protected int getCommentStartEnd(final XmlComment element) {
    return MIN_TEXT_RANGE_LENGTH;
  }

  protected boolean addToFold(List<FoldingDescriptor> foldings, PsiElement elementToFold, Document document) {
    LOG.assertTrue(elementToFold.isValid());
    TextRange range = getRangeToFold(elementToFold);
    if (range == null) return false;

    if(range.getStartOffset() >= 0 &&
       range.getEndOffset() <= elementToFold.getContainingFile().getTextRange().getEndOffset() &&
       range.getEndOffset() <= document.getTextLength() // psi and document maybe not in sync after error
      ) {

      int startLine = document.getLineNumber(range.getStartOffset());
      int endLine = document.getLineNumber(range.getEndOffset() - 1);
      if (startLine < endLine || elementToFold instanceof XmlAttribute) {
        if (range.getStartOffset() + MIN_TEXT_RANGE_LENGTH < range.getEndOffset()) {
          foldings.add(new FoldingDescriptor(elementToFold.getNode(), range));
          return true;
        }
      }
    }

    return false;
  }

  public String getPlaceholderText(@Nonnull ASTNode node) {
    final PsiElement psi = node.getPsi();
    if (psi instanceof XmlTag ||
        psi instanceof XmlComment ||
        psi instanceof XmlAttribute ||
        psi instanceof XmlConditionalSection
      ) return "...";
    return null;
  }

  public boolean isCollapsedByDefault(@Nonnull ASTNode node) {
    final PsiElement psi = node.getPsi();
    final XmlCodeFoldingSettings foldingSettings = getFoldingSettings();
    return (psi instanceof XmlTag && foldingSettings.isCollapseXmlTags())
           || (psi instanceof XmlAttribute && foldingSettings.isCollapseHtmlStyleAttribute());
  }

  private static boolean isAttributeShouldBeFolded(XmlAttribute child) {
    return child.getContainingFile() instanceof HtmlFileImpl &&
           STYLE_ATTRIBUTE.equalsIgnoreCase(child.getName());
  }

  protected abstract XmlCodeFoldingSettings getFoldingSettings();
}
