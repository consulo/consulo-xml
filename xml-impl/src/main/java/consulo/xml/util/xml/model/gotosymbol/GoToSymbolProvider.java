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

package consulo.xml.util.xml.model.gotosymbol;

import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.impl.psi.FakePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.ElementPresentationManager;
import consulo.xml.util.xml.GenericDomValue;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for "Go To Symbol" contributors.
 * <p>
 * TODO need rewrite it to better handling {@link #processNames(Processor, SearchScope, IdFilter)} and {@link #processElementsWithName(String, Processor, FindSymbolParameters)}
 */
public abstract class GoToSymbolProvider implements GotoSymbolContributor {

  protected abstract void addNames(@Nonnull Module module, Set<String> result);

  protected abstract void addItems(@Nonnull Module module, String name, List<NavigationItem> result);

  protected abstract boolean acceptModule(final Module module);

  protected static void addNewNames(@Nonnull final List<? extends DomElement> elements, final Set<String> existingNames) {
    for (DomElement name : elements) {
      existingNames.add(name.getGenericInfo().getElementName(name));
    }
  }

  @Override
  public void processNames(@Nonnull Processor<String> processor, @Nonnull SearchScope searchScope, @Nullable IdFilter idFilter) {
    if (!(searchScope instanceof ProjectAwareSearchScope)) {
      return;
    }

    Project project = ((ProjectAwareSearchScope)searchScope).getProject();

    String[] names = getNames(project, false);
    for (String name : names) {
      if (!processor.process(name)) {
        return;
      }
    }
  }

  @Override
  public void processElementsWithName(@Nonnull String name,
                                      @Nonnull Processor<NavigationItem> processor,
                                      @Nonnull FindSymbolParameters params) {
    NavigationItem[] itemsByName = getItemsByName(name,
                                                  params.getCompletePattern(),
                                                  params.getProject(),
                                                  params.isSearchInLibraries());

    for (NavigationItem navigationItem : itemsByName) {
      if (!processor.process(navigationItem)) {
        return;
      }
    }
  }

  @Nonnull
  public String[] getNames(final Project project, boolean includeNonProjectItems) {
    Set<String> result = new HashSet<String>();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      if (acceptModule(module)) {
        addNames(module, result);
      }
    }

    return ArrayUtil.toStringArray(result);
  }

  @Nonnull
  public NavigationItem[] getItemsByName(final String name, final String pattern, final Project project, boolean includeNonProjectItems) {
    List<NavigationItem> result = new ArrayList<NavigationItem>();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      if (acceptModule(module)) {
        addItems(module, name, result);
      }
    }

    return result.toArray(new NavigationItem[result.size()]);
  }

  @Nullable
  protected static NavigationItem createNavigationItem(final DomElement domElement) {
    final GenericDomValue name = domElement.getGenericInfo().getNameDomElement(domElement);
    assert name != null;
    final XmlElement psiElement = name.getXmlElement();
    final String value = name.getStringValue();
    if (psiElement == null || value == null) {
      return null;
    }
    final Image icon = ElementPresentationManager.getIcon(domElement);
    return createNavigationItem(psiElement, value, icon);
  }

  @Nonnull
  protected static NavigationItem createNavigationItem(@Nonnull final PsiElement element,
                                                       @Nonnull @NonNls final String text,
                                                       @Nullable final Image icon) {
    return new BaseNavigationItem(element, text, icon);
  }


  /**
   * Wraps one entry to display in "Go To Symbol" dialog.
   */
  public static class BaseNavigationItem extends FakePsiElement {

    private final PsiElement myPsiElement;
    private final String myText;
    private final Image myIcon;

    /**
     * Creates a new display item.
     *
     * @param psiElement The PsiElement to navigate to.
     * @param text       Text to show for this element.
     * @param icon       Icon to show for this element.
     */
    public BaseNavigationItem(@Nonnull PsiElement psiElement, @Nonnull @NonNls String text, @Nullable Image icon) {
      myPsiElement = psiElement;
      myText = text;
      myIcon = icon;
    }

    @Nonnull
    public PsiElement getNavigationElement() {
      return myPsiElement;
    }

    public Image getIcon() {
      return myIcon;
    }

    public ItemPresentation getPresentation() {
      return new ItemPresentation() {

        public String getPresentableText() {
          return myText;
        }

        @Nullable
        public String getLocationString() {
          return '(' + myPsiElement.getContainingFile().getName() + ')';
        }

        @Nullable
        public Image getIcon() {
          return myIcon;
        }
      };
    }

    public PsiElement getParent() {
      return myPsiElement.getParent();
    }

    @Nonnull
    @Override
    public Project getProject() {
      return myPsiElement.getProject();
    }

    @Override
    public PsiFile getContainingFile() {
      return myPsiElement.getContainingFile();
    }

    @Override
    public boolean isValid() {
      return myPsiElement.isValid();
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final BaseNavigationItem that = (BaseNavigationItem)o;

      if (!myPsiElement.equals(that.myPsiElement)) return false;
      if (!myText.equals(that.myText)) return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = myPsiElement.hashCode();
      result = 31 * result + myText.hashCode();
      return result;
    }
  }

}