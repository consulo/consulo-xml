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
package consulo.xml.refactoring.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.util.NonCodeSearchDescriptionLocation;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTag;

/**
 * @author yole
 */
@ExtensionImpl
public class XmlNonCodeSearchElementDescriptionProvider implements ElementDescriptionProvider {
    @Override
    @RequiredReadAction
    public LocalizeValue getElementDescription(PsiElement element, ElementDescriptionLocation location) {
        if (!(location instanceof NonCodeSearchDescriptionLocation ncdLocation)) {
            return LocalizeValue.empty();
        }
        if (ncdLocation.isNonJava()) {
            return LocalizeValue.empty();
        }
        if (element instanceof XmlTag tag) {
            return LocalizeValue.of(tag.getValue().getTrimmedText());
        }
        else if (element instanceof XmlAttribute attr) {
            return LocalizeValue.ofNullable(attr.getValue());
        }
        else if (element instanceof XmlAttributeValue attrValue) {
            return LocalizeValue.of(attrValue.getValue());
        }
        return LocalizeValue.empty();
    }
}
