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
import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.MutableLookupElement;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.parameterInfo.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.meta.PsiMetaData;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlToken;
import consulo.xml.psi.xml.XmlTokenType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

/**
 * @author Maxim.Mossienko
 */
public abstract class XmlBasedParameterInfoHandler implements ParameterInfoHandler<XmlTag, XmlElementDescriptor> {
    private static final Comparator<XmlAttributeDescriptor> COMPARATOR = Comparator.comparing(PsiMetaData::getName);

    @Override
    public Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context) {
        if (!(item instanceof MutableLookupElement)) {
            return null;
        }
        Object lookupItem = item.getObject();
        if (lookupItem instanceof XmlElementDescriptor) {
            return new Object[]{lookupItem};
        }
        return null;
    }

    @Override
    public Object[] getParametersForDocumentation(XmlElementDescriptor p, ParameterInfoContext context) {
        return getSortedDescriptors(p);
    }

    public static XmlAttributeDescriptor[] getSortedDescriptors(XmlElementDescriptor p) {
        XmlAttributeDescriptor[] xmlAttributeDescriptors = p.getAttributesDescriptors(null);
        Arrays.sort(xmlAttributeDescriptors, COMPARATOR);
        return xmlAttributeDescriptors;
    }

    @Override
    public boolean couldShowInLookup() {
        return true;
    }

    @Override
    @RequiredReadAction
    public XmlTag findElementForParameterInfo(CreateParameterInfoContext context) {
        XmlTag tag = findXmlTag(context.getFile(), context.getOffset());
        XmlElementDescriptor descriptor = tag != null ? tag.getDescriptor() : null;

        if (descriptor == null) {
            return null;
        }

        context.setItemsToShow(new Object[]{descriptor});
        return tag;
    }

    @Override
    @RequiredReadAction
    public void showParameterInfo(@Nonnull XmlTag element, CreateParameterInfoContext context) {
        context.showHint(element, element.getTextRange().getStartOffset() + 1, this);
    }

    @Override
    @RequiredReadAction
    public XmlTag findElementForUpdatingParameterInfo(UpdateParameterInfoContext context) {
        XmlTag tag = findXmlTag(context.getFile(), context.getOffset());
        if (tag != null) {
            PsiElement currentXmlTag = context.getParameterOwner();
            if (currentXmlTag == null || currentXmlTag == tag) {
                return tag;
            }
        }

        return null;
    }

    @Override
    public void updateParameterInfo(@Nonnull XmlTag o, UpdateParameterInfoContext context) {
        if (context.getParameterOwner() == null || o.equals(context.getParameterOwner())) {
            context.setParameterOwner(o);
        }
        else {
            context.removeHint();
        }
    }

    @Override
    public String getParameterCloseChars() {
        return null;
    }

    @Override
    public boolean tracksParameterIndex() {
        return false;
    }

    @Nullable
    @RequiredReadAction
    private static XmlTag findXmlTag(PsiFile file, int offset) {
        if (!(file instanceof XmlFile)) {
            return null;
        }

        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        }
        element = element.getParent();

        while (element != null) {
            if (element instanceof XmlTag tag) {
                PsiElement[] children = tag.getChildren();

                if (offset <= children[0].getTextRange().getStartOffset()) {
                    return null;
                }

                for (PsiElement child : children) {
                    TextRange range = child.getTextRange();
                    if (range.getStartOffset() <= offset && range.getEndOffset() > offset) {
                        return tag;
                    }

                    if (child instanceof XmlToken) {
                        XmlToken token = (XmlToken)child;
                        if (token.getTokenType() == XmlTokenType.XML_TAG_END) {
                            return null;
                        }
                    }
                }

                return null;
            }

            element = element.getParent();
        }

        return null;
    }

    @Override
    public void updateUI(XmlElementDescriptor o, @Nonnull ParameterInfoUIContext context) {
        updateElementDescriptor(
            o,
            context,
            new Function<>() {
                XmlTag parameterOwner = (XmlTag)context.getParameterOwner();

                @Override
                public Boolean apply(String s) {
                    return parameterOwner != null && parameterOwner.getAttributeValue(s) != null;
                }
            }
        );
    }

    public static void updateElementDescriptor(
        XmlElementDescriptor descriptor,
        ParameterInfoUIContext context,
        Function<String, Boolean> attributePresentFun
    ) {
        XmlAttributeDescriptor[] attributes = descriptor != null ? getSortedDescriptors(descriptor) : XmlAttributeDescriptor.EMPTY;

        StringBuilder sb = new StringBuilder();
        int highlightStartOffset = -1;
        int highlightEndOffset = -1;

        if (attributes.length == 0) {
            sb.append(CodeInsightLocalize.xmlTagInfoNoAttributes());
        }
        else {
            StringBuilder text1 = new StringBuilder(" ");
            StringBuilder text2 = new StringBuilder(" ");
            StringBuilder text3 = new StringBuilder(" ");

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

            sb.append(text1);
            highlightStartOffset = sb.length();
            sb.append(text2);
            highlightEndOffset = sb.length();
            sb.append(text3);
        }

        context.setupUIComponentPresentation(
            sb.toString(),
            highlightStartOffset,
            highlightEndOffset,
            false,
            false,
            true,
            context.getDefaultParameterColor()
        );
    }
}
