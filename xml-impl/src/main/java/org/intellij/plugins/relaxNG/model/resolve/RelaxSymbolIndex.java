/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.intellij.plugins.relaxNG.model.resolve;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementNavigationItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.DefaultFileTypeSpecificInputFilter;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.ex.ColoredItemPresentation;
import consulo.ui.image.Image;
import consulo.util.io.Readers;
import consulo.util.lang.CharArrayUtil;
import consulo.util.xml.fastReader.NanoXmlUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.xml.language.XmlFileType;
import consulo.xml.language.psi.XmlFile;
import org.intellij.plugins.relaxNG.ApplicationLoader;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.model.CommonElement;
import org.intellij.plugins.relaxNG.model.Define;
import org.intellij.plugins.relaxNG.model.Grammar;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author sweinreuter
 * @since 2010-06-09
 */
@ExtensionImpl
public class RelaxSymbolIndex extends ScalarIndexExtension<String> {
    public static final ID<String, Void> NAME = ID.create("RelaxSymbolIndex");

    public static Collection<String> getSymbolNames(Project project) {
        return FileBasedIndex.getInstance().getAllKeys(NAME, project);
    }

    @RequiredReadAction
    public static NavigationItem[] getSymbolsByName(String name, Project project, boolean includeNonProjectItems) {
        GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);
        Collection<NavigationItem> result = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);

        for (VirtualFile file : FileBasedIndex.getInstance().getContainingFiles(NAME, name, scope)) {
            if (psiManager.findFile(file) instanceof XmlFile xmlFile) {
                Grammar grammar = GrammarFactory.getGrammar(xmlFile);
                if (grammar != null) {
                    grammar.acceptChildren(new CommonElement.Visitor() {
                        @Override
                        public void visitDefine(Define define) {
                            if (name.equals(define.getName())) {
                                PsiElement psi = define.getPsiElement();
                                if (psi != null) {
                                    MyNavigationItem.add((NavigationItem) define.getPsiElement(), result);
                                }
                            }
                        }
                    });
                }
            }
        }
        return result.toArray(new NavigationItem[result.size()]);
    }

    @Override
    public ID<String, Void> getName() {
        return NAME;
    }

    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Void> map = new HashMap<>();
            if (inputData.getFileType() == XmlFileType.INSTANCE) {
                CharSequence inputDataContentAsText = inputData.getContentAsText();
                if (CharArrayUtil.indexOf(inputDataContentAsText, ApplicationLoader.RNG_NAMESPACE, 0) == -1) {
                    return Collections.emptyMap();
                }
                NanoXmlUtil.parse(Readers.readerFromCharSequence(inputData.getContentAsText()), new NanoXmlUtil.IXMLBuilderAdapter() {
                    NanoXmlUtil.IXMLBuilderAdapter attributeHandler;
                    int depth;

                    @Override
                    public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type) throws Exception {
                        if (attributeHandler != null) {
                            attributeHandler.addAttribute(key, nsPrefix, nsURI, value, type);
                        }
                    }

                    @Override
                    public void startElement(String name, String nsPrefix, String nsURI, String systemID, int lineNr) throws Exception {
                        attributeHandler = null;
                        if (depth == 1 && ApplicationLoader.RNG_NAMESPACE.equals(nsURI)) {
                            if ("define".equals(name)) {
                                attributeHandler = new NanoXmlUtil.IXMLBuilderAdapter() {
                                    @Override
                                    public void addAttribute(
                                        String key,
                                        String nsPrefix,
                                        String nsURI,
                                        String value,
                                        String type
                                    ) throws Exception {
                                        if ("name".equals(key) && (nsURI == null || nsURI.length() == 0) && value != null) {
                                            map.put(value, null);
                                        }
                                    }
                                };
                            }
                        }
                        depth++;
                    }

                    @Override
                    public void endElement(String name, String nsPrefix, String nsURI) throws Exception {
                        attributeHandler = null;
                        depth--;
                    }
                });
            }
            else if (inputData.getFileType() == RncFileType.getInstance()) {
                if (inputData.getPsiFile() instanceof XmlFile file) {
                    Grammar grammar = GrammarFactory.getGrammar(file);
                    if (grammar != null) {
                        grammar.acceptChildren(new CommonElement.Visitor() {
                            @Override
                            public void visitDefine(Define define) {
                                String name = define.getName();
                                if (name != null) {
                                    map.put(name, null);
                                }
                            }
                        });
                    }
                }
            }
            return map;
        };
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE, RncFileType.getInstance()) {
            @Override
            public boolean acceptInput(@Nullable Project project, VirtualFile file) {
                return !(file.getFileSystem() instanceof ArchiveFileSystem);
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    private static class MyNavigationItem implements PsiElementNavigationItem, ItemPresentation {
        private final NavigationItem myItem;
        private final ItemPresentation myPresentation;

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
            return myPresentation instanceof ColoredItemPresentation cip ? cip.getTextAttributesKey() : null;
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

        public static void add(NavigationItem item, Collection<NavigationItem> symbolNavItems) {
            ItemPresentation presentation;
            if (item instanceof PsiMetaOwner metaOwner) {
                if (metaOwner.getMetaData() instanceof PsiPresentableMetaData metaData) {
                    presentation = new ColoredItemPresentation() {
                        @Override
                        public LocalizeValue getPresentableText() {
                            return LocalizeValue.ofNullable(metaData.getName());
                        }

                        @Override
                        @RequiredReadAction
                        public @Nullable String getLocationString() {
                            return MyNavigationItem.getLocationString((PsiElement) item);
                        }

                        @Override
                        @Nullable
                        public Image getIcon() {
                            return metaData.getIcon();
                        }

                        @Nullable
                        @Override
                        public TextAttributesKey getTextAttributesKey() {
                            ItemPresentation p = item.getPresentation();
                            return p instanceof ColoredItemPresentation cip ? cip.getTextAttributesKey() : null;
                        }
                    };
                }
                else {
                    presentation = item.getPresentation();
                }
            }
            else {
                presentation = item.getPresentation();
            }

            if (presentation != null) {
                symbolNavItems.add(new MyNavigationItem(item, presentation));
            }
        }
    }
}
