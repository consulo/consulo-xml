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
package consulo.xml.util.xml.impl;

import consulo.language.impl.DebugUtil;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlEntityRef;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class PhysicalDomParentStrategy implements DomParentStrategy {
  private static final Logger LOG = Logger.getInstance("#PhysicalDomParentStrategy");
  private XmlElement myElement;
  private final DomManagerImpl myDomManager;

  public PhysicalDomParentStrategy(@Nonnull final XmlElement element, DomManagerImpl domManager) {
    myElement = element;
    myDomManager = domManager;
  }

  public DomInvocationHandler getParentHandler() {
    final XmlTag parentTag = getParentTag(myElement);
    assert parentTag != null;
    return myDomManager.getDomHandler(parentTag);
  }

  public static XmlTag getParentTag(final XmlElement xmlElement) {
    return (XmlTag) getParentTagCandidate(xmlElement);
  }

  public static PsiElement getParentTagCandidate(final XmlElement xmlElement) {
    final PsiElement parent = xmlElement.getParent();
    return parent instanceof XmlEntityRef ? parent.getParent() : parent;
  }

  @Nonnull
  public final XmlElement getXmlElement() {
    return myElement;
  }

  @Nonnull
  public DomParentStrategy refreshStrategy(final DomInvocationHandler handler) {
    return this;
  }

  @Nonnull
  public DomParentStrategy setXmlElement(@Nonnull final XmlElement element) {
    myElement = element;
    return this;
  }

  @Override
  public String toString() {
    return "Physical:" + myElement;
  }

  @Nonnull
  public DomParentStrategy clearXmlElement() {
    final DomInvocationHandler parent = getParentHandler();
    assert parent != null : "write operations should be performed on the DOM having a parent, your DOM may be not very fresh";
    return new VirtualDomParentStrategy(parent);
  }

  public String checkValidity() {
    return myElement.isValid() ? null : "Invalid PSI";
  }

  @Override
  public XmlFile getContainingFile(DomInvocationHandler handler) {
    return DomImplUtil.getFile(handler);
  }

  @Override
  public boolean isPhysical() {
    return true;
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  public boolean equals(final Object o) {
    return strategyEquals(this, o);
  }

  public static boolean strategyEquals(DomParentStrategy strategy, final Object o) {

    if (strategy == o) return true;
    if (!(o instanceof DomParentStrategy)) return false;
    final XmlElement thatElement = ((DomParentStrategy) o).getXmlElement();
    if (thatElement == null) return false;
    XmlElement element = strategy.getXmlElement();
    if (element == null) return false;

    if (xmlElementsEqual(element, thatElement)) {
      if (element != thatElement) {
        final PsiElement nav1 = element.getNavigationElement();
        final PsiElement nav2 = thatElement.getNavigationElement();
        if (nav1 != nav2) {
          PsiElement curContext = findIncluder(element);
          PsiElement navContext = findIncluder(nav1);
          LOG.error(
              "x:include processing error",
              "nav1,nav2=" + nav1 + ", " + nav2 + ";\n" +
                  nav1.getContainingFile() + ":" + nav1.getTextRange().getStartOffset() + "!=" + nav2.getContainingFile() + ":" + nav2.getTextRange().getStartOffset() + ";\n" +
                  (nav1 == element) + ";" + (nav2 == thatElement) + ";\n" +
                  "contexts equal: " + (curContext == navContext) + ";\n" +
                  "curContext?.physical=" + (curContext != null && curContext.isPhysical()) + ";\n" +
                  "navContext?.physical=" + (navContext != null && navContext.isPhysical()) + ";\n" +
                  "myElement.physical=" + element.isPhysical() + ";\n" +
                  "thatElement.physical=" + thatElement.isPhysical() + "\n" + DebugUtil.currentStackTrace(),
              AttachmentFactory.get().create("Including tag text 1.xml", curContext == null ? "null" : curContext.getText()),
              AttachmentFactory.get().create("Including tag text 2.xml", navContext == null ? "null" : navContext.getText())
          );
          throw new AssertionError();
        }
      }
      return true;
    }
    return false;
  }

  @Nullable
  private static PsiElement findIncluder(PsiElement cur) {
    while (cur != null && !cur.isPhysical()) {
      cur = cur.getParent();
    }
    return cur;
  }

  private static boolean xmlElementsEqual(@Nonnull final PsiElement fst, @Nonnull final PsiElement snd) {
    if (fst.equals(snd)) return true;

    if (fst.isValid() && fst.isPhysical() || snd.isValid() && snd.isPhysical()) return false;
    if (fst.getTextLength() != snd.getTextLength()) return false;
    if (fst.getStartOffsetInParent() != snd.getStartOffsetInParent()) return false;

    PsiElement nav1 = fst.getNavigationElement();
    PsiElement nav2 = snd.getNavigationElement();
    return nav1 != null && nav1.equals(nav2);
  }

  public int hashCode() {
    if (!myElement.isPhysical()) {
      return myElement.getNavigationElement().hashCode();
    }

    return myElement.hashCode();
  }
}
