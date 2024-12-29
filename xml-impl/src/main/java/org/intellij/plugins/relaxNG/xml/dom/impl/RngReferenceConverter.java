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

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ref.Ref;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.*;
import org.intellij.plugins.relaxNG.model.Define;
import org.intellij.plugins.relaxNG.model.resolve.DefinitionResolver;
import org.intellij.plugins.relaxNG.xml.dom.RngDefine;
import org.intellij.plugins.relaxNG.xml.dom.RngDomVisitor;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;
import org.intellij.plugins.relaxNG.xml.dom.RngInclude;

import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 18.08.2007
 */
public class RngReferenceConverter implements CustomReferenceConverter {
  @Override
  @Nonnull
  public PsiReference[] createReferences(GenericDomValue genericDomValue, PsiElement element, ConvertContext context) {
    final GenericAttributeValue<String> e = (GenericAttributeValue<String>) genericDomValue;

    if (genericDomValue.getParent() instanceof RngDefine) {
      final XmlAttributeValue value = e.getXmlAttributeValue();
      if (value == null) {
        return PsiReference.EMPTY_ARRAY;
      }

      return new PsiReference[]{
          new PsiReferenceBase<XmlAttributeValue>(value, true) {
            @Override
            public PsiElement resolve() {
//                  final XmlTag tag = PsiTreeUtil.getParentOfType(value, XmlTag.class);
//                  final XmlTag include = getAncestorTag(tag, "include", ProjectLoader.RNG_NAMESPACE);
//                  final XmlTag grammar = getAncestorTag(tag, "grammar", ProjectLoader.RNG_NAMESPACE);
//
//                  if (include != null && (grammar == null || PsiTreeUtil.isAncestor(grammar, include, true))) {
//                    final ResolveResult[] e = new DefinitionReference(getElement(), false).multiResolve(false);
//                  }
              return myElement.getParent().getParent();
            }

            @Override
            @Nonnull
            public Object[] getVariants() {
              final RngInclude include = e.getParentOfType(RngInclude.class, true);
              final RngGrammar scope = e.getParentOfType(RngGrammar.class, true);
              if (scope != null && include != null && DomUtil.isAncestor(scope, include, true)) {
                final XmlFile file = include.getIncludedFile().getValue();
                if (file != null) {
                  final DomFileElement<DomElement> fileElement = scope.getManager().getFileElement(file, DomElement.class);
                  if (fileElement == null) {
                    return EMPTY_ARRAY;
                  }

                  final Ref<Object[]> ref = new Ref<>(ArrayUtil.EMPTY_STRING_ARRAY);
                  fileElement.acceptChildren(new RngDomVisitor() {
                    @Override
                    public void visit(RngGrammar grammar) {
                      final Map<String, Set<Define>> map = DefinitionResolver.getAllVariants(grammar);
                      if (map != null) {
                        ref.set(map.keySet().toArray());
                      }
                    }
                  });
                  return ref.get();
                }
              }
              return ArrayUtil.EMPTY_STRING_ARRAY; // TODO: look for unresolved refs;
            }
          }
      };
    }

    return new PsiReference[]{
        new DefinitionReference(e)
    };
  }
}
