/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.rename.RenameInputValidator;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.xml.patterns.XmlPatterns;
import consulo.xml.psi.xml.XmlAttributeDecl;
import consulo.xml.psi.xml.XmlElementDecl;

import static consulo.language.pattern.PlatformPatterns.psiElement;
import static consulo.language.pattern.StandardPatterns.or;

@ExtensionImpl
public class XmlElementRenameValidator implements RenameInputValidator {
  @Override
  public ElementPattern<? extends PsiElement> getPattern() {
    return or(
        XmlPatterns.xmlTag().withMetaData(
            or(PlatformPatterns.instanceOf(XmlElementDescriptor.class),
                PlatformPatterns.instanceOf(XmlAttributeDescriptor.class))
        ),
        psiElement(XmlElementDecl.class),
        psiElement(XmlAttributeDecl.class),
        XmlPatterns.xmlTag().withDescriptor(
            or(PlatformPatterns.instanceOf(XmlElementDescriptor.class),
                PlatformPatterns.instanceOf(XmlAttributeDescriptor.class))
        )
    );
  }

  public boolean isInputValid(final String newName, final PsiElement element, final ProcessingContext context) {
    return newName.trim().matches("([\\d\\w\\_\\.\\-]+:)?[\\d\\w\\_\\.\\-]+");
  }
}
