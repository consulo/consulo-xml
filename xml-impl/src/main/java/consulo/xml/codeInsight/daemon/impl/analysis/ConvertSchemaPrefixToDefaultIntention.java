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
import consulo.xml.psi.XmlRecursiveElementVisitor;
import consulo.xml.psi.impl.source.xml.SchemaPrefix;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;

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
    public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException {
        final XmlAttribute xmlns = getXmlnsDeclaration(element);
        if (xmlns == null) {
            return;
        }
        SchemaPrefixReference prefixRef = null;
        for (PsiReference ref : xmlns.getReferences()) {
            if (ref instanceof SchemaPrefixReference) {
                prefixRef = (SchemaPrefixReference)ref;
                break;
            }
        }
        if (prefixRef == null) {
            return;
        }

        final SchemaPrefix prefix = prefixRef.resolve();
        final String ns = prefixRef.getNamespacePrefix();
        final ArrayList<XmlTag> tags = new ArrayList<XmlTag>();
        final ArrayList<XmlAttribute> attrs = new ArrayList<XmlAttribute>();
        xmlns.getParent().accept(new XmlRecursiveElementVisitor() {
            @Override
            public void visitXmlTag(XmlTag tag) {
                if (ns.equals(tag.getNamespacePrefix())) {
                    tags.add(tag);
                }
                super.visitXmlTag(tag);
            }

            @Override
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
        new WriteCommandAction(project, "Convert namespace prefix to default", xmlns.getContainingFile()) {
            @Override
            protected void run(Result result) throws Throwable {
                final int index = ns.length() + 1;
                for (XmlTag tag : tags) {
                    final String s = tag.getName().substring(index);
                    if (s.length() > 0) {
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
    public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
        return getXmlnsDeclaration(element) != null;
    }

    @Nullable
    private static XmlAttribute getXmlnsDeclaration(PsiElement element) {
        final PsiElement parent = element.getParent();
        if (parent == null) {
            return null;
        }
        for (PsiReference ref : parent.getReferences()) {
            if (ref instanceof SchemaPrefixReference) {
                final PsiElement elem = ref.resolve();
                if (elem != null) {
                    final PsiElement attr = elem.getParent();
                    if (attr instanceof XmlAttribute) {
                        final PsiElement tag = attr.getParent();
                        if (tag instanceof XmlTag && ((XmlTag)tag).getAttribute("xmlns") == null) {
                            return (XmlAttribute)attr;
                        }
                    }
                }
            }
        }
        return null;
    }
}
