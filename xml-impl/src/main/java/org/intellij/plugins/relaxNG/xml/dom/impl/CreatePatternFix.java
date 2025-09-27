/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.relaxNG.xml.dom.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import jakarta.annotation.Nonnull;

import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.xml.psi.XmlElementFactory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;
import consulo.language.util.IncorrectOperationException;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.intention.IntentionAction;
import consulo.logging.Logger;
import org.intellij.plugins.relaxNG.ApplicationLoader;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;

/**
 * XXX: the tests rely on this still being an intention action
 *
 * @author sweinreuter
 * @since 2007-07-17
 */
class CreatePatternFix implements IntentionAction, LocalQuickFix {
    private final PsiReference myReference;

    public CreatePatternFix(PsiReference reference) {
        myReference = reference;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getText() {
        return "Create Pattern '" + myReference.getCanonicalText() + "'";
    }

    @Override
    @Nonnull
    public String getFamilyName() {
        return "Create Pattern";
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getName() {
        return getText();
    }

    @Override
    @RequiredWriteAction
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        if (!isAvailable()) {
            return;
        }
        try {
            doFix();
        }
        catch (consulo.language.util.IncorrectOperationException e) {
            Logger.getInstance(getClass().getName()).error(e);
        }
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return isAvailable();
    }

    @RequiredReadAction
    private boolean isAvailable() {
        if (!(myReference instanceof DefinitionReference) || !myReference.getElement().isValid()) {
            return false;
        }
        else {
            RngGrammar grammar = ((DefinitionReference) myReference).getScope();
            return grammar != null && grammar.getXmlTag() != null;
        }
    }

    @Override
    @RequiredWriteAction
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws consulo.language.util.IncorrectOperationException {
        doFix();
    }

    @RequiredWriteAction
    private void doFix() throws IncorrectOperationException {
        XmlTag tag = PsiTreeUtil.getParentOfType(myReference.getElement(), XmlTag.class);
        assert tag != null;
        XmlTag defineTag = tag.createChildTag("define", ApplicationLoader.RNG_NAMESPACE, "\n \n", false);
        defineTag.setAttribute("name", myReference.getCanonicalText());

        RngGrammar grammar = ((DefinitionReference) myReference).getScope();
        if (grammar == null) {
            return;
        }
        XmlTag root = grammar.getXmlTag();
        if (root == null) {
            return;
        }

        XmlTag[] tags = root.getSubTags();
        for (XmlTag xmlTag : tags) {
            if (PsiTreeUtil.isAncestor(xmlTag, tag, false)) {
                XmlElementFactory ef = XmlElementFactory.getInstance(tag.getProject());
                XmlText text = ef.createDisplayText(" ");
                PsiElement e = root.addAfter(text, xmlTag);

                root.addAfter(defineTag, e);
                return;
            }
        }
        root.add(defineTag);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static XmlTag getAncestorTag(XmlTag tag, String name, String namespace) {
        if (tag == null) {
            return null;
        }
        if (tag.getLocalName().equals(name) && tag.getNamespace().equals(namespace)) {
            return tag;
        }
        return getAncestorTag(tag.getParentTag(), name, namespace);
    }
}
