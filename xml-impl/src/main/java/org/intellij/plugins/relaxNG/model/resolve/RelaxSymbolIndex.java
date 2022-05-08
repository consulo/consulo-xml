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

import consulo.xml.ide.highlighter.XmlFileType;
import consulo.xml.psi.xml.XmlFile;
import consulo.colorScheme.TextAttributesKey;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementNavigationItem;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.DefaultFileTypeSpecificInputFilter;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.ScalarIndexExtension;
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
import org.intellij.plugins.relaxNG.ApplicationLoader;
import org.intellij.plugins.relaxNG.compact.RncFileType;
import org.intellij.plugins.relaxNG.model.CommonElement;
import org.intellij.plugins.relaxNG.model.Define;
import org.intellij.plugins.relaxNG.model.Grammar;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/*
* Created by IntelliJ IDEA.
* User: sweinreuter
* Date: 09.06.2010
*/
public class RelaxSymbolIndex extends ScalarIndexExtension<String> {
  @NonNls
  public static final ID<String, Void> NAME = ID.create("RelaxSymbolIndex");

  public static Collection<String> getSymbolNames(Project project) {
    return FileBasedIndex.getInstance().getAllKeys(NAME, project);
  }

  public static NavigationItem[] getSymbolsByName(final String name, Project project, boolean includeNonProjectItems) {
    final GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);
    final Collection<NavigationItem> result = new ArrayList<>();
    PsiManager psiManager = PsiManager.getInstance(project);

    for(VirtualFile file: FileBasedIndex.getInstance().getContainingFiles(NAME, name, scope)) {
      final PsiFile psiFile = psiManager.findFile(file);

      if (psiFile instanceof XmlFile) {
        final Grammar grammar = GrammarFactory.getGrammar((XmlFile)psiFile);

        if (grammar != null) {
          grammar.acceptChildren(new CommonElement.Visitor() {
            @Override
            public void visitDefine(Define define) {
              if (name.equals(define.getName())) {
                final PsiElement psi = define.getPsiElement();
                if (psi != null) {
                  MyNavigationItem.add((NavigationItem)define.getPsiElement(), result);
                }
              }
            }
          });
        }
      }
    }
    return result.toArray(new NavigationItem[result.size()]);
  }

  @Nonnull
  @Override
  public ID<String, Void> getName() {
    return NAME;
  }

  @Nonnull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return new DataIndexer<String, Void, FileContent>() {
      @Override
      @Nonnull
      public Map<String, Void> map(@Nonnull FileContent inputData) {
        final HashMap<String, Void> map = new HashMap<>();
        if (inputData.getFileType() == XmlFileType.INSTANCE) {
          CharSequence inputDataContentAsText = inputData.getContentAsText();
          if (CharArrayUtil.indexOf(inputDataContentAsText, ApplicationLoader.RNG_NAMESPACE, 0) == -1) return Collections.emptyMap();
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
                    public void addAttribute(String key, String nsPrefix, String nsURI, String value, String type) throws Exception {
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
        } else if (inputData.getFileType() == RncFileType.getInstance()) {
          final PsiFile file = inputData.getPsiFile();
          if (file instanceof XmlFile) {
            final Grammar grammar = GrammarFactory.getGrammar((XmlFile)file);
            if (grammar != null) {
              grammar.acceptChildren(new CommonElement.Visitor() {
                @Override
                public void visitDefine(Define define) {
                  final String name = define.getName();
                  if (name != null) {
                    map.put(name, null);
                  }
                }
              });
            }
          }
        }
        return map;
      }
    };
  }

  @Nonnull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  @Nonnull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return new DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE, RncFileType.getInstance()) {
      @Override
      public boolean acceptInput(@Nullable Project project, @Nonnull VirtualFile file) {
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

    private MyNavigationItem(NavigationItem item, @Nonnull final ItemPresentation presentation) {
      myItem = item;
      myPresentation = presentation;
    }

    @Override
    public String getPresentableText() {
      return myPresentation.getPresentableText();
    }

    @Override
    @Nullable
    public String getLocationString() {
      return getLocationString((PsiElement)myItem);
    }

    private static String getLocationString(PsiElement element) {
      return "(in " + element.getContainingFile().getName() + ")";
    }

    @Override
    @Nullable
    public Image getIcon() {
      return myPresentation.getIcon();
    }

    @Nullable
    public TextAttributesKey getTextAttributesKey() {
      return myPresentation instanceof ColoredItemPresentation ? ((ColoredItemPresentation) myPresentation).getTextAttributesKey() : null;
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
    public void navigate(boolean requestFocus) {
      myItem.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
      return myItem.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
      return myItem.canNavigateToSource();
    }

    public static void add(final NavigationItem item, Collection<NavigationItem> symbolNavItems) {
      final ItemPresentation presentation;
      if (item instanceof PsiMetaOwner) {
        final PsiMetaData data = ((PsiMetaOwner)item).getMetaData();
        if (data instanceof PsiPresentableMetaData) {
          final PsiPresentableMetaData metaData = (PsiPresentableMetaData)data;
          presentation = new ColoredItemPresentation() {
            @Override
            public String getPresentableText() {
              return metaData.getName();
            }

            @Override
            @Nullable
            public String getLocationString() {
              return MyNavigationItem.getLocationString((PsiElement)item);
            }

            @Override
            @Nullable
            public Image getIcon() {
              return metaData.getIcon();
            }

            @Nullable
            @Override
            public TextAttributesKey getTextAttributesKey() {
              final ItemPresentation p = item.getPresentation();
              return p instanceof ColoredItemPresentation ? ((ColoredItemPresentation) p).getTextAttributesKey() : null;
            }
          };
        } else {
          presentation = item.getPresentation();
        }
      } else {
        presentation = item.getPresentation();
      }

      if (presentation != null) {
        symbolNavItems.add(new MyNavigationItem(item, presentation));
      }
    }
  }
}
