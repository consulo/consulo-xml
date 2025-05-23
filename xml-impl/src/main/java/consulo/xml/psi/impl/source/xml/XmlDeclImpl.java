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
package consulo.xml.psi.impl.source.xml;

import jakarta.annotation.Nonnull;

import consulo.language.psi.PsiElementVisitor;
import consulo.xml.psi.XmlElementVisitor;
import consulo.xml.psi.xml.XmlDecl;
import consulo.xml.psi.xml.XmlElementType;

/**
 * @author Mike
 */
public class XmlDeclImpl extends XmlElementImpl implements XmlDecl{
  public XmlDeclImpl() {
    super(XmlElementType.XML_DECL);
  }

  public void accept(@Nonnull PsiElementVisitor visitor) {
    if (visitor instanceof XmlElementVisitor) {
      ((XmlElementVisitor)visitor).visitXmlDecl(this);
    }
    else {
      visitor.visitElement(this);
    }
  }
}
