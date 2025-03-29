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
package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.psi.XmlRecursiveElementVisitor;
import consulo.xml.psi.impl.source.xml.SchemaPrefix;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "xml.convert.scheme.prefix.to.default", fileExtensions = "xml", categories = "XML")
public class ConvertSchemaPrefixToDefaultIntention extends PsiElementBaseIntentionAction {
    public static final String NAME = "Set Namespace Prefix to Empty";

    public ConvertSchemaPrefixToDefaultIntention() {
        setText(NAME);
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException {
        XmlAttribute xmlns = getXmlnsDeclaration(element);
        if (xmlns == null) {
            return;
        }
        SchemaPrefixReference prefixRef = null;
        for (PsiReference ref : xmlns.getReferences()) {
            if (ref instanceof SchemaPrefixReference schemaPrefixReference) {
                prefixRef = schemaPrefixReference;
                break;
            }
        }
        if (prefixRef == null) {
            return;
        }

        SchemaPrefix prefix = prefixRef.resolve();
        String ns = prefixRef.getNamespacePrefix();
        List<XmlTag> tags = new ArrayList<>();
        List<XmlAttribute> attrs = new ArrayList<>();
        xmlns.getParent().accept(new XmlRecursiveElementVisitor() {
            @Override
            public void visitXmlTag(XmlTag tag) {
                if (ns.equals(tag.getNamespacePrefix())) {
                    tags.add(tag);
                }
                super.visitXmlTag(tag);
            }

            @Override
            @RequiredReadAction
            public void visitXmlAttributeValue(XmlAttributeValue value) {
                if (value.getValue().startsWith(ns + ":")) {
                    for (PsiReference ref : value.getReferences()) {
                        if (ref instanceof SchemaPrefixReference && ref.isReferenceTo(prefix)) {
                            attrs.add((XmlAttribute)value.getParent());
                        }
                    }
                }
            }
        });
        new WriteCommandAction(
            project,
            "Convert namespace prefix to default",
            xmlns.getContainingFile()
        ) {
            @Override
            @RequiredWriteAction
            protected void run(Result result) throws Throwable {
                int index = ns.length() + 1;
                for (XmlTag tag : tags) {
                    String s = tag.getName().substring(index);
                    if (!s.isEmpty()) {
                        tag.setName(s);
                    }
                }
                for (XmlAttribute attr : attrs) {
                    //noinspection ConstantConditions
                    attr.setValue(attr.getValue().substring(index));
                }
                xmlns.setName("xmlns");
            }
        }.execute();
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
        return getXmlnsDeclaration(element) != null;
    }

    @Nullable
    @RequiredReadAction
    private static XmlAttribute getXmlnsDeclaration(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent == null) {
            return null;
        }
        for (PsiReference ref : parent.getReferences()) {
            if (ref instanceof SchemaPrefixReference schemaPrefixReference) {
                PsiElement elem = schemaPrefixReference.resolve();
                if (elem != null
                    && elem.getParent() instanceof XmlAttribute attr
                    && attr.getParent() instanceof XmlTag xmlTag
                    && xmlTag.getAttribute("xmlns") == null) {
                    return attr;
                }
            }
        }
        return null;
    }
}
