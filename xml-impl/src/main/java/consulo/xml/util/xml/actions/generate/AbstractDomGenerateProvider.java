/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.xml.util.xml.actions.generate;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.language.psi.PsiFile;
import consulo.application.util.matcher.NameUtil;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomElementNavigationProvider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public abstract class AbstractDomGenerateProvider<T extends DomElement> extends DefaultGenerateElementProvider<T> {
  public static final String NAMESPACE_PREFIX_VAR = "NS_PREFIX";

  @Nullable
  private final String myMappingId;

  public AbstractDomGenerateProvider(final String description, final Class<T> aClass) {
    this(description, aClass, null);
  }

  public AbstractDomGenerateProvider(final String description, final Class<T> aClass, @Nullable String mappingId) {
    super(description, aClass);
    myMappingId = mappingId;
  }

  public T generate(final Project project, final Editor editor, final PsiFile file) {
    DomElement parentDomElement = getParentDomElement(project, editor, file);

    final T t = generate(parentDomElement, editor);

    runTemplate(editor, file, t, getPredefinedVars(parentDomElement, t, editor, file));

    return t;
  }

  protected Map<String, String> getPredefinedVars(@Nullable DomElement parentDomElement,
                                                  @Nullable T t,
                                                  @Nonnull Editor editor,
                                                  @Nonnull PsiFile file) {
    return createNamespacePrefixMap(parentDomElement);
  }

  @Nonnull
  public static Map<String, String> createNamespacePrefixMap(@Nullable DomElement domElement) {
    Map<String, String> vars = new HashMap<String, String>();

    addNamespacePrefix(domElement, vars);

    return vars;
  }

  public static void addNamespacePrefix(@Nullable DomElement domElement, @Nonnull Map<String, String> vars) {
    if (domElement != null) {
      XmlTag tag = domElement.getXmlTag();
      if (tag != null) {
        String namespacePrefix = tag.getNamespacePrefix();
        if (!StringUtil.isEmptyOrSpaces(namespacePrefix)) {
          vars.put(NAMESPACE_PREFIX_VAR, namespacePrefix + ":");
        }
      }
    }
  }

  protected void runTemplate(final Editor editor, final PsiFile file, final T t, Map<String, String> predefinedVars) {
    DomTemplateRunner.getInstance(file.getProject()).runTemplate(t, myMappingId, editor, predefinedVars);
  }

  protected abstract DomElement getParentDomElement(final Project project, final Editor editor, final PsiFile file);

  @SuppressWarnings({"unchecked"})
  protected void doNavigate(final DomElementNavigationProvider navigateProvider, final DomElement copy) {
    final DomElement element = getElementToNavigate((T)copy);
    if (element != null) {
      super.doNavigate(navigateProvider, element);
    }
  }

  @Nullable
  protected DomElement getElementToNavigate(final T t) {
    return t;
  }

  protected static String getDescription(final Class<? extends DomElement> aClass) {
    return StringUtil.join(Arrays.asList(NameUtil.nameToWords(aClass.getSimpleName())), " ");
  }

  @Nullable
  public String getMappingId() {
    return myMappingId;
  }
}
