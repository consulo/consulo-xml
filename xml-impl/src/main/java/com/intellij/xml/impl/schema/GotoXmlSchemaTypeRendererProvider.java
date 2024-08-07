/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.xml.impl.schema;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.navigation.GotoTargetRendererProvider;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.xml.psi.impl.source.xml.XmlTagImpl;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 7/5/12
 * Time: 8:27 PM
 */
@ExtensionImpl
public class GotoXmlSchemaTypeRendererProvider implements GotoTargetRendererProvider {
    @Override
    public PsiElementListCellRenderer getRenderer(PsiElement element) {
        if (element instanceof XmlTagImpl tag) {
            if (SchemaDefinitionsSearch.isTypeElement(tag)) {
                return new MyRenderer("");
            }
            else if (SchemaDefinitionsSearch.isElementWithSomeEmbeddedType(tag)) {
                return new MyRenderer("xsd:element: ");
            }
        }
        return null;
    }

    private static class MyRenderer extends PsiElementListCellRenderer<XmlTagImpl> {
        private final String myPrefix;

        private MyRenderer(String prefix) {
            myPrefix = prefix;
        }

        @Override
        public String getElementText(XmlTagImpl element) {
            final XmlAttribute attr = SchemaDefinitionsSearch.getNameAttr(element);
            return myPrefix + (attr == null || attr.getValue() == null ? element.getName() : attr.getValue());
        }

        @Override
        protected String getContainerText(XmlTagImpl element, String name) {
            final PsiFile file = element.getContainingFile();
            return "(" + file.getName() + ")";
        }

        @Override
        protected int getIconFlags() {
            return 0;
        }
    }
}
