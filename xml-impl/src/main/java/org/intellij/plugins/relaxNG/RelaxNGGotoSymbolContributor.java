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
package org.intellij.plugins.relaxNG;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.colorScheme.TextAttributesKey;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementNavigationItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdFilter;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.ui.ex.ColoredItemPresentation;
import consulo.ui.image.Image;
import consulo.xml.language.psi.XmlFile;
import org.intellij.plugins.relaxNG.model.CommonElement;
import org.intellij.plugins.relaxNG.model.Define;
import org.intellij.plugins.relaxNG.model.Grammar;
import org.intellij.plugins.relaxNG.model.resolve.GrammarFactory;
import org.intellij.plugins.relaxNG.model.resolve.RelaxSymbolIndex;

import org.jspecify.annotations.Nullable;

import java.util.Collections;

/**
 * @author sweinreuter
 * @since 2007-10-24
 */
@ExtensionImpl
public class RelaxNGGotoSymbolContributor implements GotoSymbolContributor {
    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        FileBasedIndex.getInstance().processAllKeys(RelaxSymbolIndex.NAME, processor, scope, filter);
    }

    @Override
    @RequiredReadAction
    public void processElementsWithName(
        String name,
        Processor<NavigationItem> processor,
        FindSymbolParameters parameters
    ) {
        boolean[] result = {true};
        PsiManager psiManager = PsiManager.getInstance(parameters.getProject());
        FileBasedIndex.getInstance().getFilesWithKey(
            RelaxSymbolIndex.NAME,
            Collections.singleton(name),
            file -> {
                Grammar grammar = psiManager.findFile(file) instanceof XmlFile xmlFile ? GrammarFactory.getGrammar(xmlFile) : null;
                if (grammar == null) {
                    return true;
                }
                grammar.acceptChildren(new CommonElement.Visitor() {
                    @Override
                    public void visitDefine(Define define) {
                        if (!result[0]) {
                            return;
                        }
                        if (name.equals(define.getName())) {
                            NavigationItem wrapped = wrap(define.getPsiElement());
                            result[0] = wrapped == null || processor.process(wrapped);
                        }
                    }
                });
                return result[0];
            },
            parameters.getSearchScope()
        );
    }

    @Nullable
    static NavigationItem wrap(@Nullable PsiElement item) {
        if (!(item instanceof NavigationItem navItem)) {
            return null;
        }
        PsiMetaData metaData0 = navItem instanceof PsiMetaOwner metaOwner ? metaOwner.getMetaData() : null;
        PsiPresentableMetaData metaData = metaData0 instanceof PsiPresentableMetaData presentableMetaData ? presentableMetaData : null;
        ItemPresentation presentation = metaData != null ? new ColoredItemPresentation() {
            @Override
            public LocalizeValue getPresentableText() {
                return LocalizeValue.ofNullable(metaData.getName());
            }

            @Override
            @RequiredReadAction
            public String getLocationString() {
                return MyNavigationItem.getLocationString(item);
            }

            @Override
            @Nullable
            public Image getIcon() {
                return metaData.getIcon();
            }

            @Nullable
            @Override
            public TextAttributesKey getTextAttributesKey() {
                ItemPresentation p = navItem.getPresentation();
                return p instanceof ColoredItemPresentation cip ? cip.getTextAttributesKey() : null;
            }
        } : navItem.getPresentation();
        return presentation == null ? null : new MyNavigationItem(navItem, presentation);
    }

    private static final class MyNavigationItem implements PsiElementNavigationItem, ItemPresentation {
        final NavigationItem myItem;
        final ItemPresentation myPresentation;

        private MyNavigationItem(NavigationItem item, ItemPresentation presentation) {
            myItem = item;
            myPresentation = presentation;
        }

        @Override
        public LocalizeValue getPresentableText() {
            return myPresentation.getPresentableText();
        }

        @Override
        @RequiredReadAction
        public @Nullable String getLocationString() {
            return getLocationString((PsiElement) myItem);
        }

        @RequiredReadAction
        private static String getLocationString(PsiElement element) {
            return "(in " + element.getContainingFile().getName() + ")";
        }

        @Override
        public @Nullable Image getIcon() {
            return myPresentation.getIcon();
        }

        @Nullable
        public TextAttributesKey getTextAttributesKey() {
            return myPresentation instanceof ColoredItemPresentation coloredItemPresentation
                ? coloredItemPresentation.getTextAttributesKey()
                : null;
        }

        @Override
        public String getName() {
            return myItem.getName();
        }

        @Override
        public ItemPresentation getPresentation() {
            return this;
        }

        @Override
        public PsiElement getTargetElement() {
            return (PsiElement) myItem;
        }

        @Override
        @RequiredReadAction
        public void navigate(boolean requestFocus) {
            myItem.navigate(requestFocus);
        }

        @Override
        @RequiredReadAction
        public boolean canNavigate() {
            return myItem.canNavigate();
        }

        @Override
        @RequiredReadAction
        public boolean canNavigateToSource() {
            return myItem.canNavigateToSource();
        }
    }
}