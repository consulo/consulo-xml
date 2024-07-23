package com.intellij.xml.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.util.lang.Comparing;
import consulo.xml.psi.xml.XmlElement;
import com.intellij.xml.util.XmlEnumeratedValueReference;
import consulo.util.lang.StringUtil;

/**
 * @author Dmitry Avdeev
 * Date: 22.08.13
 */
public abstract class XmlEnumerationDescriptor<T extends XmlElement> {
    public abstract boolean isFixed();

    public abstract String getDefaultValue();

    public abstract boolean isEnumerated(@Nullable XmlElement context);

    public abstract String[] getEnumeratedValues();

    public String[] getValuesForCompletion() {
        return StringUtil.filterEmptyStrings(getEnumeratedValues());
    }

    public PsiElement getValueDeclaration(XmlElement attributeValue, String value) {
        String defaultValue = getDefaultValue();
        if (Comparing.equal(defaultValue, value)) {
            return getDefaultValueDeclaration();
        }
        return isFixed() ? null : getEnumeratedValueDeclaration(attributeValue, value);
    }

    protected abstract PsiElement getEnumeratedValueDeclaration(XmlElement value, String s);

    protected abstract PsiElement getDefaultValueDeclaration();

    public PsiReference[] getValueReferences(T element, @Nonnull String text) {
        return new PsiReference[]{new XmlEnumeratedValueReference(element, this)};
    }
}
