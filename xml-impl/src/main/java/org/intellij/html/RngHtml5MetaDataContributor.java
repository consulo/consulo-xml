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
package org.intellij.html;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.meta.MetaDataRegistrar;
import org.intellij.plugins.relaxNG.compact.psi.RncDecl;
import org.intellij.plugins.relaxNG.compact.psi.RncFile;
import org.intellij.plugins.relaxNG.compact.psi.RncNsDecl;
import org.intellij.plugins.relaxNG.compact.psi.impl.RncDocument;
import consulo.language.psi.filter.AndFilter;
import consulo.language.psi.filter.ClassFilter;
import consulo.xml.psi.filters.XmlTextFilter;
import consulo.language.psi.meta.MetaDataContributor;
import consulo.util.lang.reflect.ReflectionUtil;
import com.intellij.xml.util.XmlUtil;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class RngHtml5MetaDataContributor implements MetaDataContributor {
  @Override
  public void contributeMetaData(MetaDataRegistrar registrar) {
    registrar.registerMetaData(
      new AndFilter(
        new ClassFilter(RncDocument.class),
        new MyRncNamespaceFilter(XmlUtil.HTML_URI, XmlUtil.XHTML_URI)),
      RelaxedHtmlFromRngNSDescriptor::new
    );
  }

  private static class MyRncNamespaceFilter extends XmlTextFilter {
    MyRncNamespaceFilter(String... namespaces) {
      super(namespaces);
    }

    @Override
    public boolean isClassAcceptable(Class hintClass) {
      return ReflectionUtil.isAssignable(RncDocument.class, hintClass);
    }

    @Override
    public boolean isAcceptable(Object element, PsiElement context) {
      if (!(element instanceof RncDocument)) {
        return false;
      }

      final PsiFile file = ((RncDocument)element).getContainingFile();
      String namespace = null;
      if (file instanceof RncFile) {
        for (RncDecl decl : ((RncFile)file).getDeclarations()) {
          if (decl instanceof RncNsDecl) {
            namespace = decl.getDeclaredNamespace();
            break;
          }
        }
      }

      if (namespace != null) {
        for (String aMyValue : myValue) {
          if (aMyValue.equals(namespace)) return true;
        }
      }
      return false;
    }
  }
}
