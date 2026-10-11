/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.xml.lang.html.structureView;

import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.localize.LocalizeValue;
import consulo.navigation.LocationPresentation;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.xml.ide.structureView.impl.xml.XmlTagTreeElement;
import consulo.xml.language.psi.XmlTag;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

class HtmlTagTreeElement extends PsiTreeElementBase<XmlTag> implements LocationPresentation {
    static final int MAX_TEXT_LENGTH = 50;

    public HtmlTagTreeElement(XmlTag tag) {
        super(tag);
    }

    @Override
    @RequiredReadAction
    public Collection<StructureViewTreeElement> getChildrenBase() {
        XmlTag tag = getElement();
        if (tag == null || !tag.isValid()) {
            return Collections.emptyList();
        }

        return ContainerUtil.map2List(tag.getSubTags(), HtmlTagTreeElement::new);
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        XmlTag tag = getElement();
        if (tag == null) {
            return IdeLocalize.nodeStructureviewInvalid();
        }

        return LocalizeValue.of(getTagPresentation(tag));
    }

    @Override
    @RequiredReadAction
    public String getLocationString() {
        XmlTag tag = getElement();
        if (tag == null) {
            return null;
        }

        String text = normalizeSpacesAndShortenIfLong(tag.getValue().getTrimmedText());
        return text.isEmpty() ? null : text;
    }

    @Override
    public boolean isSearchInLocationString() {
        return true;
    }

    public static String getTagPresentation(XmlTag tag) {
        String id = XmlTagTreeElement.toCanonicalForm(tag.getAttributeValue("id"));

        String classValue = tag.getAttributeValue("class");
        List<String> classValues = classValue != null ? StringUtil.split(classValue, " ") : Collections.<String>emptyList();

        StringBuilder text = new StringBuilder(tag.getLocalName());

        if (id != null) {
            text.append("#").append(id);
        }

        if (!classValues.isEmpty()) {
            text.append('.').append(StringUtil.join(classValues, "."));
        }

        return text.toString();
    }

    public static String normalizeSpacesAndShortenIfLong(String text) {
        return shortenTextIfLong(normalizeSpaces(text));
    }

    private static String normalizeSpaces(String text) {
        StringBuilder buf = new StringBuilder();

        for (char ch : text.toCharArray()) {
            if (ch <= ' ' || Character.isSpaceChar(ch)) {
                if (buf.length() == 0 || buf.charAt(buf.length() - 1) != ' ') {
                    buf.append(' ');
                }
            }
            else {
                buf.append(ch);
            }
        }

        return buf.toString();
    }

    private static String shortenTextIfLong(String text) {
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }

        int index;
        for (index = MAX_TEXT_LENGTH; index > MAX_TEXT_LENGTH - 20; index--) {
            if (!Character.isLetter(text.charAt(index))) {
                break;
            }
        }

        int endIndex = Character.isLetter(text.charAt(index)) ? MAX_TEXT_LENGTH : index;
        return text.substring(0, endIndex) + "...";
    }

    @Override
    public String getLocationPrefix() {
        return "  ";
    }

    @Override
    public String getLocationSuffix() {
        return "";
    }
}
