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
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.xml.psi.XmlRecursiveElementVisitor;
import consulo.xml.psi.impl.source.resolve.reference.impl.providers.TypeOrElementOrAttributeReference;
import consulo.xml.psi.impl.source.xml.SchemaPrefixReference;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "xml.insert.namespace.prefix", fileExtensions = "xml", categories = "XML")
public class AddSchemaPrefixIntention extends PsiElementBaseIntentionAction {
    public static final String NAME = "Insert Namespace Prefix";

    public AddSchemaPrefixIntention() {
        setText(LocalizeValue.localizeTODO(NAME));
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException {
        XmlAttribute xmlns = getXmlnsDeclaration(element);
        if (xmlns == null) {
            return;
        }
        String namespace = xmlns.getValue();
        XmlTag tag = xmlns.getParent();

        if (tag != null) {
            Set<String> ns = tag.getLocalNamespaceDeclarations().keySet();
            String nsPrefix = Messages.showInputDialog(
                project,
                "Namespace Prefix:",
                NAME,
                UIUtil.getInformationIcon(),
                "",
                new InputValidator() {
                    @Override
                    @RequiredUIAccess
                    public boolean checkInput(String inputString) {
                        return !ns.contains(inputString);
                    }

                    @Override
                    @RequiredUIAccess
                    public boolean canClose(String inputString) {
                        return checkInput(inputString);
                    }
                }
            );
            if (nsPrefix == null) {
                return;
            }
            List<XmlTag> tags = new ArrayList<>();
            List<XmlAttributeValue> values = new ArrayList<>();
            new WriteCommandAction(project, NAME, tag.getContainingFile()) {
                @Override
                @RequiredWriteAction
                protected void run(Result result) throws Throwable {
                    tag.accept(new XmlRecursiveElementVisitor() {
                        @Override
                        public void visitXmlTag(XmlTag tag) {
                            if (tag.getNamespace().equals(namespace) && tag.getNamespacePrefix().length() == 0) {
                                tags.add(tag);
                            }
                            super.visitXmlTag(tag);
                        }

                        @Override
                        @RequiredReadAction
                        public void visitXmlAttributeValue(XmlAttributeValue value) {
                            PsiReference ref = null;
                            boolean skip = false;
                            for (PsiReference reference : value.getReferences()) {
                                if (reference instanceof TypeOrElementOrAttributeReference) {
                                    ref = reference;
                                }
                                else if (reference instanceof SchemaPrefixReference) {
                                    skip = true;
                                    break;
                                }
                            }
                            if (!skip && ref != null && ref.resolve() instanceof XmlElement element) {
                                XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
                                if (tag != null && tag.getNamespace().equals(namespace)
                                    && ref.getRangeInElement().getLength() == value.getValue().length()) { //no ns prefix
                                    values.add(value);
                                }
                            }
                        }
                    });
                    for (XmlAttributeValue value : values) {
                        ((XmlAttribute)value.getParent()).setValue(nsPrefix + ":" + value.getValue());
                    }
                    for (XmlTag xmlTag : tags) {
                        xmlTag.setName(nsPrefix + ":" + xmlTag.getLocalName());
                    }
                    xmlns.setName("xmlns:" + nsPrefix);
                }
            }.execute();
        }
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
        return getXmlnsDeclaration(element) != null;
    }

    @Nullable
    private static XmlAttribute getXmlnsDeclaration(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof XmlTag tag) {
            if (tag.getNamespacePrefix().isEmpty()) {
                while (tag != null) {
                    XmlAttribute attr = tag.getAttribute("xmlns");
                    if (attr != null) {
                        return attr;
                    }
                    tag = tag.getParentTag();
                }
            }
        }
        else if (parent instanceof XmlAttribute attribute && "xmlns".equals(attribute.getName())) {
            return attribute;
        }
        return null;
    }
}
