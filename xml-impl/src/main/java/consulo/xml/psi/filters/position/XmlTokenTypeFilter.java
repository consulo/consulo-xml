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
package consulo.xml.psi.filters.position;

import consulo.language.ast.IElementType;
import consulo.xml.psi.xml.XmlToken;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.ElementFilter;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 10.03.2003
 * Time: 12:10:08
 * To change this template use Options | File Templates.
 */
public class XmlTokenTypeFilter implements ElementFilter {
  private IElementType myType = null;

  public XmlTokenTypeFilter(){}

  public XmlTokenTypeFilter(IElementType type){
    myType = type;
  }

  public boolean isClassAcceptable(Class hintClass){
    return ReflectionUtil.isAssignable(XmlToken.class, hintClass);
  }

  public boolean isAcceptable(Object element, PsiElement context){
    if(element instanceof PsiElement) {
      final ASTNode node = ((PsiElement)element).getNode();
      return node != null && node.getElementType() == myType;
    }
    else if(element instanceof ASTNode){
      return ((ASTNode)element).getElementType() == myType;
    }
    return false;
  }

  public String toString(){
    return "token-type(" + myType + ")";
  }
}