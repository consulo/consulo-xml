package com.intellij.xml.util;

import consulo.language.psi.PsiReference;
import com.intellij.psi.impl.source.xml.SchemaPrefixReference;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import consulo.language.util.ProcessingContext;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.impl.schema.XmlAttributeDescriptorImpl;
import com.intellij.xml.impl.schema.XmlNSDescriptorImpl;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceProvider;

import javax.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 */
public class XmlPrefixReferenceProvider extends PsiReferenceProvider {

  @Nonnull
  @Override
  public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
    XmlAttributeValue attributeValue = (XmlAttributeValue)element;
    PsiElement parent = attributeValue.getParent();
    if (parent instanceof XmlAttribute && !XmlNSDescriptorImpl.checkSchemaNamespace(((XmlAttribute)parent).getParent())) {
      XmlAttributeDescriptor descriptor = ((XmlAttribute)parent).getDescriptor();
      if (descriptor instanceof XmlAttributeDescriptorImpl) {
        String type = ((XmlAttributeDescriptorImpl)descriptor).getType();
        if (type != null && type.endsWith(":QName")) {
          String prefix = XmlUtil.findPrefixByQualifiedName(type);
          String ns = ((XmlTag)descriptor.getDeclaration()).getNamespaceByPrefix(prefix);
          if (XmlNSDescriptorImpl.checkSchemaNamespace(ns)) {
            String value = attributeValue.getValue();
            if (value != null) {
              int i = value.indexOf(':');
              if (i > 0) {
                return new PsiReference[] {
                  new SchemaPrefixReference(attributeValue, TextRange.from(1, i), value.substring(0, i), null)
                };
              }
            }
          }
        }
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }
}
