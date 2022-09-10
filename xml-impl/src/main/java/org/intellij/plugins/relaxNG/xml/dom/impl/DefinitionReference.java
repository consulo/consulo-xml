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

import consulo.language.editor.completion.lookup.LookupValueFactory;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixProvider;
import consulo.language.psi.*;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.GenericAttributeValue;
import org.intellij.plugins.relaxNG.model.Define;
import org.intellij.plugins.relaxNG.model.resolve.DefinitionResolver;
import org.intellij.plugins.relaxNG.xml.dom.RngGrammar;
import org.intellij.plugins.relaxNG.xml.dom.RngParentRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by IntelliJ IDEA.
 * User: sweinreuter
 * Date: 18.08.2007
 */
public class DefinitionReference extends PsiReferenceBase.Poly<XmlAttributeValue>
        implements LocalQuickFixProvider,
    EmptyResolveMessageProvider, Function<Define, ResolveResult>
{

  private final boolean myIsParentRef;
  private final GenericAttributeValue<String> myValue;

  public DefinitionReference(GenericAttributeValue<String> value) {
    super(value.getXmlAttributeValue());
    myValue = value;
    myIsParentRef = value.getParent() instanceof RngParentRef;
  }

  @Override
  public boolean isSoft() {
    return true;
  }

  @Override
  @Nonnull
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    final RngGrammar scope = getScope();
    if (scope == null) {
      return ResolveResult.EMPTY_ARRAY;
    }

    final Set<Define> set = DefinitionResolver.resolve(scope, myValue.getValue());
    if (set == null || set.size() == 0) return ResolveResult.EMPTY_ARRAY;

    return ContainerUtil.map2Array(set, ResolveResult.class, this);
  }

  @Nullable
  public RngGrammar getScope() {
    RngGrammar scope = myValue.getParentOfType(RngGrammar.class, true);
    if (scope == null) {
      return null;
    }
    if (myIsParentRef) {
      scope = scope.getParentOfType(RngGrammar.class, true);
    }
    return scope;
  }

  @Override
  public ResolveResult apply(Define define) {
    final XmlElement xmlElement = (XmlElement)define.getPsiElement();
    assert xmlElement != null;
    return new PsiElementResolveResult(xmlElement);
  }

  @Override
  @Nonnull
  public Object[] getVariants() {
    final RngGrammar scope = getScope();
    if (scope == null) {
      return ResolveResult.EMPTY_ARRAY;
    }

    final Map<String, Set<Define>> map = DefinitionResolver.getAllVariants(scope);
    if (map == null || map.size() == 0) return ArrayUtil.EMPTY_OBJECT_ARRAY;

    return ContainerUtil.mapNotNull(map.values(), defines -> {
      final Define define = defines.iterator().next();
      if (defines.size() == 0) {
        return null;
      } else {
        final PsiElement element = define.getPsiElement();
        if (element != null) {
          final PsiPresentableMetaData data = (PsiPresentableMetaData)((PsiMetaOwner)element).getMetaData();
          if (data != null) {
            return LookupValueFactory.createLookupValue(data.getName(), data.getIcon());
          } else {
            return define.getName();
          }
        } else {
          return define.getName();
        }
      }
    }).toArray();
  }

  @Override
  public LocalQuickFix[] getQuickFixes() {
    final XmlTag tag = PsiTreeUtil.getParentOfType(getElement(), XmlTag.class);
    assert tag != null;
    final RngGrammar scope = myValue.getParentOfType(RngGrammar.class, true);
    if (scope != null) {
      return new LocalQuickFix[]{ new CreatePatternFix(this) };
    }
    return LocalQuickFix.EMPTY_ARRAY;
  }

  @Override
  @Nonnull
  public String getUnresolvedMessagePattern() {
    return "Unresolved pattern reference ''{0}''";
  }
}
