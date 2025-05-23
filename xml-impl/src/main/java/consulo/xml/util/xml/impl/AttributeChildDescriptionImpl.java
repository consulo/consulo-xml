package consulo.xml.util.xml.impl;

import consulo.util.collection.ArrayUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.reflect.DomAttributeChildDescription;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * @author peter
 */
public class AttributeChildDescriptionImpl extends DomChildDescriptionImpl implements DomAttributeChildDescription<Void> {
  private final JavaMethod myGetterMethod;

  protected AttributeChildDescriptionImpl(final XmlName attributeName, @Nonnull final JavaMethod getter) {
    super(attributeName, getter.getGenericReturnType());
    myGetterMethod = getter;
  }

  public AttributeChildDescriptionImpl(final XmlName attributeName, @Nonnull Type type) {
    super(attributeName, type);
    myGetterMethod = null;
  }

  @Nonnull
  public DomNameStrategy getDomNameStrategy(@Nonnull DomElement parent) {
    final DomNameStrategy strategy = DomImplUtil.getDomNameStrategy(ReflectionUtil.getRawType(getType()), true);
    return strategy == null ? parent.getNameStrategy() : strategy;
  }


  public final JavaMethod getGetterMethod() {
    return myGetterMethod;
  }

  @Override
  public String toString() {
    return "Attribute:" + getXmlName();
  }

  @Nullable
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    final JavaMethod method = getGetterMethod();
    return method == null ? super.getAnnotation(annotationClass) : method.getAnnotation(annotationClass);
  }

  @Nonnull
  public List<? extends DomElement> getValues(@Nonnull DomElement parent) {
    return Arrays.asList(getDomAttributeValue(parent));
  }

  @Nonnull
  public String getCommonPresentableName(@Nonnull DomNameStrategy strategy) {
    throw new UnsupportedOperationException("Method getCommonPresentableName is not yet implemented in " + getClass().getName());
  }

  public GenericAttributeValue getDomAttributeValue(DomElement parent) {
    final DomInvocationHandler handler = DomManagerImpl.getDomInvocationHandler(parent);
    if (handler != null) {
      return getDomAttributeValue(handler);
    }
    return (GenericAttributeValue)myGetterMethod.invoke(parent, ArrayUtil.EMPTY_OBJECT_ARRAY);
  }

  public GenericAttributeValue getDomAttributeValue(final DomInvocationHandler handler) {
    return (GenericAttributeValue)handler.getAttributeChild(this).getProxy();
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    final AttributeChildDescriptionImpl that = (AttributeChildDescriptionImpl)o;

    if (myGetterMethod != null ? !myGetterMethod.equals(that.myGetterMethod) : that.myGetterMethod != null) return false;

    return true;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 29 * result + (myGetterMethod != null ? myGetterMethod.hashCode() : 0);
    return result;
  }
}
