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
package consulo.xml.codeInsight.hint.api.impls;

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import consulo.document.util.TextRange;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.MutableLookupElement;
import consulo.language.editor.parameterInfo.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

/**
 * @author Maxim.Mossienko
 */
public abstract class XmlBasedParameterInfoHandler implements ParameterInfoHandler<XmlTag,XmlElementDescriptor> {
  private static final Comparator<XmlAttributeDescriptor> COMPARATOR = new Comparator<XmlAttributeDescriptor>() {
    public int compare(final XmlAttributeDescriptor o1, final XmlAttributeDescriptor o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  public Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context) {
    if (!(item instanceof MutableLookupElement)) return null;
    final Object lookupItem = item.getObject();
    if (lookupItem instanceof XmlElementDescriptor) return new Object[]{lookupItem};
    return null;
  }

  public Object[] getParametersForDocumentation(final XmlElementDescriptor p, final ParameterInfoContext context) {
    return getSortedDescriptors(p);
  }

  public static XmlAttributeDescriptor[] getSortedDescriptors(final XmlElementDescriptor p) {
    final XmlAttributeDescriptor[] xmlAttributeDescriptors = p.getAttributesDescriptors(null);
    Arrays.sort(xmlAttributeDescriptors, COMPARATOR);
    return xmlAttributeDescriptors;
  }

  public boolean couldShowInLookup() {
    return true;
  }

  public XmlTag findElementForParameterInfo(final CreateParameterInfoContext context) {
    final XmlTag tag = findXmlTag(context.getFile(), context.getOffset());
    final XmlElementDescriptor descriptor = tag != null ? tag.getDescriptor() : null;

    if (descriptor == null) {
      return null;
    }

    context.setItemsToShow(new Object[] {descriptor});
    return tag;
  }

  public void showParameterInfo(final @Nonnull XmlTag element, final CreateParameterInfoContext context) {
    context.showHint(element, element.getTextRange().getStartOffset() + 1, this);
  }

  public XmlTag findElementForUpdatingParameterInfo(final UpdateParameterInfoContext context) {
    final XmlTag tag = findXmlTag(context.getFile(), context.getOffset());
    if (tag != null) {
      final PsiElement currentXmlTag = context.getParameterOwner();
      if (currentXmlTag == null || currentXmlTag == tag) return tag;
    }

    return null;
  }

  public void updateParameterInfo(@Nonnull final XmlTag o, final UpdateParameterInfoContext context) {
    if (context.getParameterOwner() == null || o.equals(context.getParameterOwner())) {
      context.setParameterOwner( o );
    } else {
      context.removeHint();
    }
  }

  public String getParameterCloseChars() {
    return null;
  }

  public boolean tracksParameterIndex() {
    return false;
  }

  @Nullable
  private static XmlTag findXmlTag(PsiFile file, int offset){
    if (!(file instanceof XmlFile)) return null;

    PsiElement element = file.findElementAt(offset);
    if (element == null) return null;
    element = element.getParent();

    while (element != null) {
      if (element instanceof XmlTag) {
        XmlTag tag = (XmlTag)element;

        final PsiElement[] children = tag.getChildren();

        if (offset <= children[0].getTextRange().getStartOffset()) return null;

        for (PsiElement child : children) {
          final TextRange range = child.getTextRange();
          if (range.getStartOffset() <= offset && range.getEndOffset() > offset) return tag;

          if (child instanceof XmlToken) {
            XmlToken token = (XmlToken)child;
            if (token.getTokenType() == XmlTokenType.XML_TAG_END) return null;
          }
        }

        return null;
      }

      element = element.getParent();
    }

    return null;
  }

  public void updateUI(XmlElementDescriptor o, final ParameterInfoUIContext context) {
    updateElementDescriptor(
      o,
      context,
      new Function<String, Boolean>() {
        final XmlTag parameterOwner  = (XmlTag)context.getParameterOwner();

        public Boolean apply(String s) {
          return parameterOwner != null ? parameterOwner.getAttributeValue(s) != null:false;
        }
      });
  }

  public static void updateElementDescriptor(XmlElementDescriptor descriptor, ParameterInfoUIContext context,
                                             Function<String, Boolean> attributePresentFun) {
    final XmlAttributeDescriptor[] attributes = descriptor != null ? getSortedDescriptors(descriptor) : XmlAttributeDescriptor.EMPTY;

    StringBuffer buffer = new StringBuffer();
    int highlightStartOffset = -1;
    int highlightEndOffset = -1;

    if (attributes.length == 0) {
      buffer.append(CodeInsightBundle.message("xml.tag.info.no.attributes"));
    }
    else {
      StringBuffer text1 = new StringBuffer(" ");
      StringBuffer text2 = new StringBuffer(" ");
      StringBuffer text3 = new StringBuffer(" ");

      for (XmlAttributeDescriptor attribute : attributes) {
        if (Boolean.TRUE.equals(attributePresentFun.apply(attribute.getName()))) {
          if (!(text1.toString().equals(" "))) {
            text1.append(", ");
          }
          text1.append(attribute.getName());
        }
        else if (attribute.isRequired()) {
          if (!(text2.toString().equals(" "))) {
            text2.append(", ");
          }
          text2.append(attribute.getName());
        }
        else {
          if (!(text3.toString().equals(" "))) {
            text3.append(", ");
          }
          text3.append(attribute.getName());
        }
      }

      if (!text1.toString().equals(" ") && !text2.toString().equals(" ")) {
        text1.append(", ");
      }

      if (!text2.toString().equals(" ") && !text3.toString().equals(" ")) {
        text2.append(", ");
      }

      if (!text1.toString().equals(" ") && !text3.toString().equals(" ") && text2.toString().equals(" ")) {
        text1.append(", ");
      }

      buffer.append(text1);
      highlightStartOffset = buffer.length();
      buffer.append(text2);
      highlightEndOffset = buffer.length();
      buffer.append(text3);
    }

    context.setupUIComponentPresentation(buffer.toString(), highlightStartOffset, highlightEndOffset, false,
                                         false, true, context.getDefaultParameterColor());
  }
}
