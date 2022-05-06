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
package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import consulo.language.psi.OuterLanguageElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.filter.ElementFilter;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.util.XmlUtil;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.util.ProcessingContext;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
public class IdReferenceProvider extends PsiReferenceProvider {
  @NonNls public static final String FOR_ATTR_NAME = "for";
  @NonNls public static final String ID_ATTR_NAME = "id";
  @NonNls public static final String STYLE_ID_ATTR_NAME = "styleId";
  @NonNls public static final String NAME_ATTR_NAME = "name";

  private static final Set<String> ourNamespacesWithoutNameReference = new HashSet<String>();
  static {
    ourNamespacesWithoutNameReference.add( XmlUtil.JSP_URI );
    ourNamespacesWithoutNameReference.add( XmlUtil.STRUTS_BEAN_URI );
    ourNamespacesWithoutNameReference.add( XmlUtil.STRUTS_BEAN_URI2 );
    ourNamespacesWithoutNameReference.add( XmlUtil.STRUTS_LOGIC_URI );
    for(String s: XmlUtil.JSTL_CORE_URIS) ourNamespacesWithoutNameReference.add( s );
    ourNamespacesWithoutNameReference.add( "http://struts.apache.org/tags-tiles" );
    for(String s: XmlUtil.SCHEMA_URIS) ourNamespacesWithoutNameReference.add( s );
  }

  public String[] getIdForAttributeNames() {
    return new String[]{FOR_ATTR_NAME, ID_ATTR_NAME, NAME_ATTR_NAME,STYLE_ID_ATTR_NAME};
  }

  public ElementFilter getIdForFilter() {
    return new ElementFilter() {
      public boolean isAcceptable(Object element, PsiElement context) {
        final PsiElement grandParent = ((PsiElement)element).getParent().getParent();
        if (grandParent instanceof XmlTag) {
          final XmlTag tag = (XmlTag)grandParent;

          if (tag.getNamespacePrefix().length() > 0) {
            return true;
          }
        }
        return false;
      }

      public boolean isClassAcceptable(Class hintClass) {
        return true;
      }
    };
  }

  @Nonnull
  public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull final ProcessingContext context) {
    if (element instanceof XmlAttributeValue) {
      final XmlExtension extension = XmlExtension.getExtensionByElement(element);
      if (extension != null && extension.hasDynamicComponents(element)) {
        return PsiReference.EMPTY_ARRAY;
      }

      final PsiElement parentElement = element.getParent();
      if (!(parentElement instanceof XmlAttribute)) return PsiReference.EMPTY_ARRAY;
      final String name = ((XmlAttribute)parentElement).getName();
      final String ns = ((XmlAttribute)parentElement).getParent().getNamespace();
      final boolean jsfNs = XmlUtil.JSF_CORE_URI.equals(ns) || XmlUtil.JSF_HTML_URI.equals(ns);

      if (FOR_ATTR_NAME.equals(name)) {
        return new PsiReference[]{
          jsfNs && element.getText().indexOf(':') == -1 ?
          new IdRefReference(element):
          new IdRefReference(element) {
            public boolean isSoft() {
              final XmlAttributeDescriptor descriptor = ((XmlAttribute)parentElement).getDescriptor();
              return descriptor != null ? !descriptor.hasIdRefType() : false;
            }
          }
        };
      }
      else {
        final boolean allowReferences = !(ourNamespacesWithoutNameReference.contains(ns));
        
        if ((ID_ATTR_NAME.equals(name) && allowReferences) ||
             STYLE_ID_ATTR_NAME.equals(name) ||
             (NAME_ATTR_NAME.equals(name) && allowReferences)
            ) {
          final AttributeValueSelfReference attributeValueSelfReference;

          if (jsfNs) {
            attributeValueSelfReference = new AttributeValueSelfReference(element);
          } else {
            if (hasOuterLanguageElement(element)) return PsiReference.EMPTY_ARRAY;

            attributeValueSelfReference =  new GlobalAttributeValueSelfReference(element, true);
          }
          return new PsiReference[]{attributeValueSelfReference};
        }
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }

  private static boolean hasOuterLanguageElement(@Nonnull PsiElement element) {
    for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof OuterLanguageElement) {
        return true;
      }
    }

    return false;
  }

  public static class GlobalAttributeValueSelfReference extends AttributeValueSelfReference {
    private final boolean mySoft;

    public GlobalAttributeValueSelfReference(PsiElement element, boolean soft) {
      super(element);
      mySoft = soft;
    }

    public boolean isSoft() {
      return mySoft;
    }
  }
}
