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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.ide.navigation.GotoTargetPresentationProvider;
import consulo.language.editor.ui.navigation.PsiTargetPresentationFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.navigation.TargetPresentation;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.psi.impl.source.xml.XmlTagImpl;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author Irina.Chernushina
 * @since 2012-07-05
 */
@ExtensionImpl
public class GotoXmlSchemaTypePresentationProvider implements GotoTargetPresentationProvider {
    private final PsiTargetPresentationFactory myPsiTargetPresentationFactory;

    @Inject
    public GotoXmlSchemaTypePresentationProvider(PsiTargetPresentationFactory psiTargetPresentationFactory) {
        myPsiTargetPresentationFactory = psiTargetPresentationFactory;
    }

    @Override
    @RequiredReadAction
    public @Nullable TargetPresentation getPresentation(PsiElement element, Options options) {
        if (!(element instanceof XmlTagImpl tag)) {
            return null;
        }

        String prefix;
        if (SchemaDefinitionsSearch.isTypeElement(tag)) {
            prefix = "";
        }
        else if (SchemaDefinitionsSearch.isElementWithSomeEmbeddedType(tag)) {
            prefix = "xsd:element: ";
        }
        else {
            return null;
        }

        XmlAttribute attr = SchemaDefinitionsSearch.getNameAttr(tag);
        String name = attr == null || attr.getValue() == null ? tag.getName() : attr.getValue();

        PsiFile file = tag.getContainingFile();

        return myPsiTargetPresentationFactory
            .presentationBuilder(tag)
            .withPresentableText(LocalizeValue.of(prefix + name))
            .withContainerText(LocalizeValue.of("(" + file.getName() + ")"))
            .build();
    }
}
