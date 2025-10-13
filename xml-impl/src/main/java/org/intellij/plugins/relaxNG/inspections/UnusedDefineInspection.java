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
package org.intellij.plugins.relaxNG.inspections;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.query.Query;
import consulo.language.ast.ASTNode;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import jakarta.annotation.Nonnull;
import org.intellij.plugins.relaxNG.ApplicationLoader;
import org.intellij.plugins.relaxNG.compact.psi.RncDefine;
import org.intellij.plugins.relaxNG.compact.psi.RncElementVisitor;
import org.intellij.plugins.relaxNG.compact.psi.RncGrammar;
import org.intellij.plugins.relaxNG.compact.psi.impl.RncDefineImpl;
import org.intellij.plugins.relaxNG.model.resolve.RelaxIncludeIndex;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;

/**
 * @author sweinreuter
 * @since 2007-07-26
 */
@ExtensionImpl
public class UnusedDefineInspection extends BaseInspection {
    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Unused Define");
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "UnusedDefine";
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    @Override
    @Nonnull
    public RncElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new MyElementVisitor(holder);
    }

    private static final class MyElementVisitor extends RncElementVisitor {
        private final ProblemsHolder myHolder;

        private final XmlElementVisitor myXmlVisitor = new XmlElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitXmlTag(XmlTag tag) {
                MyElementVisitor.this.visitXmlTag(tag);
            }
        };

        public MyElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        protected void superVisitElement(PsiElement element) {
            element.accept(myXmlVisitor);
        }

        @Override
        @RequiredReadAction
        public void visitDefine(RncDefine pattern) {
            RncGrammar grammar = PsiTreeUtil.getParentOfType(pattern, RncGrammar.class);
            PsiFile file = pattern.getContainingFile();
            if (grammar != null) {
                if (processRncUsages(pattern, new LocalSearchScope(grammar))) {
                    return;
                }
            }
            else {
                if (processRncUsages(pattern, new LocalSearchScope(file))) {
                    return;
                }
            }

            PsiElementProcessor.CollectElements<XmlFile> collector = new PsiElementProcessor.CollectElements<>();
            RelaxIncludeIndex.processBackwardDependencies((XmlFile) file, collector);

            if (processRncUsages(pattern, new LocalSearchScope(collector.toArray()))) {
                return;
            }

            ASTNode astNode = ((RncDefineImpl) pattern).getNameNode();
            myHolder.newProblem(LocalizeValue.of("Unreferenced define"))
                .range(astNode.getPsi())
                .withFix(new MyFix<>(pattern))
                .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                .create();
        }

        @RequiredReadAction
        private static boolean processRncUsages(PsiElement tag, LocalSearchScope scope) {
            Query<PsiReference> query = ReferencesSearch.search(tag, scope);
            for (PsiReference reference : query) {
                PsiElement e = reference.getElement();
                RncDefine t = PsiTreeUtil.getParentOfType(e, RncDefine.class, false);
                if (t == null || !PsiTreeUtil.isAncestor(tag, t, true)) {
                    return true;
                }
            }
            return false;
        }

        @RequiredReadAction
        public void visitXmlTag(XmlTag tag) {
            PsiFile file = tag.getContainingFile();
            if (file.getFileType() != XmlFileType.INSTANCE) {
                return;
            }
            if (!"define".equals(tag.getLocalName())) {
                return;
            }
            if (!ApplicationLoader.RNG_NAMESPACE.equals(tag.getNamespace())) {
                return;
            }
            if (tag.getAttribute("combine") != null) {
                return; // ?
            }

            XmlAttribute attr = tag.getAttribute("name");
            if (attr == null) {
                return;
            }

            XmlAttributeValue value = attr.getValueElement();
            if (value == null) {
                return;
            }

            String s = value.getValue();
            if (s == null || s.length() == 0) {
                return;
            }
            PsiElement parent = value.getParent();
            if (!(parent instanceof XmlAttribute attribute && "name".equals(attribute.getName()))) {
                return;
            }
            if (!(parent.getParent() instanceof XmlTag)) {
                return;
            }

            DomElement element = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
            if (element == null) {
                return;
            }

            RngGrammar rngGrammar = element.getParentOfType(RngGrammar.class, true);
            if (rngGrammar != null) {
                if (processUsages(tag, value, new LocalSearchScope(rngGrammar.getXmlTag()))) {
                    return;
                }
            }
            else {
                if (processUsages(tag, value, new LocalSearchScope(file))) {
                    return;
                }
            }

            PsiElementProcessor.CollectElements<XmlFile> collector = new PsiElementProcessor.CollectElements<>();
            RelaxIncludeIndex.processBackwardDependencies((XmlFile) file, collector);

            if (processUsages(tag, value, new LocalSearchScope(collector.toArray()))) {
                return;
            }

            myHolder.newProblem(LocalizeValue.of("Unreferenced define"))
                .range(value)
                .withFix(new MyFix<>(tag))
                .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                .create();
        }

        @RequiredReadAction
        private static boolean processUsages(PsiElement tag, XmlAttributeValue value, LocalSearchScope scope) {
            Query<PsiReference> query = ReferencesSearch.search(tag, scope, true);
            for (PsiReference reference : query) {
                PsiElement e = reference.getElement();
                if (e != value) {
                    XmlTag t = PsiTreeUtil.getParentOfType(e, XmlTag.class);
                    if (t != null && !PsiTreeUtil.isAncestor(tag, t, true)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static class MyFix<T extends PsiElement> implements LocalQuickFix {
            private final T myTag;

            public MyFix(T tag) {
                myTag = tag;
            }

            @Nonnull
            @Override
            public LocalizeValue getName() {
                return LocalizeValue.localizeTODO("Remove define");
            }

            @Override
            @RequiredWriteAction
            public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
                try {
                    if (myTag.isValid()) {
                        myTag.delete();
                    }
                }
                catch (IncorrectOperationException e) {
                    Logger.getInstance(UnusedDefineInspection.class.getName()).error(e);
                }
            }
        }
    }
}
