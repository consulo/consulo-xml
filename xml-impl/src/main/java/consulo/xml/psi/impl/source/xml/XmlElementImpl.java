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

/*
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Aug 26, 2002
 * Time: 6:25:08 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package consulo.xml.psi.impl.source.xml;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlElementType;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.util.XmlPsiUtil;
import consulo.content.scope.SearchScope;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.psi.CompositePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.scope.GlobalSearchScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class XmlElementImpl extends CompositePsiElement implements XmlElement {
  public XmlElementImpl(IElementType type) {
    super(type);
  }

  public boolean processElements(PsiElementProcessor processor, PsiElement place){
    return XmlPsiUtil.processXmlElements(this, processor, false);
  }

  public boolean processChildren(PsiElementProcessor processor){
    return XmlPsiUtil.processXmlElementChildren(this, processor, false);
  }

  public XmlElement findElementByTokenType(final IElementType type){
    final XmlElement[] result = new XmlElement[1];
    result[0] = null;

    processElements(new PsiElementProcessor(){
      public boolean execute(@Nonnull PsiElement element){
        if(element instanceof TreeElement && ((ASTNode)element).getElementType() == type){
          result[0] = (XmlElement)element;
          return false;
        }
        return true;
      }
    }, this);

    return result[0];
  }

  public PsiElement getContext() {
    final XmlElement data = getUserData(INCLUDING_ELEMENT);
    if(data != null) return data;
    return getAstParent();
  }

  private PsiElement getAstParent() {
    return super.getParent();
  }

  @Nonnull
  public PsiElement getNavigationElement() {
    if (!isPhysical()) {
      final XmlElement including = getUserData(INCLUDING_ELEMENT);
      if (including != null) {
        return including;
      }
      PsiElement astParent = getAstParent();
      PsiElement parentNavigation = astParent.getNavigationElement();
      if (parentNavigation.getTextOffset() == getTextOffset()) return parentNavigation;
      return this;
    }
    return super.getNavigationElement();
  }

  public PsiElement getParent(){
    return getContext();
  }

  @Nonnull
  public Language getLanguage() {
    return getContainingFile().getLanguage();
  }

  @Nullable
  protected static String getNameFromEntityRef(final CompositeElement compositeElement, final IElementType xmlEntityDeclStart) {
    final ASTNode node = compositeElement.findChildByType(xmlEntityDeclStart);
    if (node == null) return null;
    ASTNode name = node.getTreeNext();

    if (name != null && name.getElementType() == TokenType.WHITE_SPACE) {
      name = name.getTreeNext();
    }

    if (name != null && name.getElementType() == XmlElementType.XML_ENTITY_REF) {
      final StringBuilder builder = new StringBuilder();

      ((XmlElement)name.getPsi()).processElements(new PsiElementProcessor() {
        public boolean execute(@Nonnull final PsiElement element) {
          builder.append(element.getText());
          return true;
        }
      }, name.getPsi());
      if (builder.length() > 0) return builder.toString();
    }
    return null;
  }

  @Nonnull
  public SearchScope getUseScope() {
    return GlobalSearchScope.allScope(getProject());
  }

  @Override
  public boolean isEquivalentTo(final PsiElement another) {

    if (super.isEquivalentTo(another)) return true;
    PsiElement element1 = this;
    PsiElement element2 = another;

    // TODO: seem to be only necessary for tag dirs equivalens checking.
    if (element1 instanceof XmlTag && element2 instanceof XmlTag) {
      if (!element1.isPhysical() && !element2.isPhysical()) return element1.getText().equals(element2.getText());
    }

    return false;
  }
}
