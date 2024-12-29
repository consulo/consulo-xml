package consulo.xml.util.xml.impl;

import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.language.util.IncorrectOperationException;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author peter
 */
public class CollectionChildDescriptionImpl extends DomChildDescriptionImpl implements DomCollectionChildDescription, AbstractCollectionChildDescription {
  private final Collection<JavaMethod> myGetterMethods;
  private final NotNullFunction<DomInvocationHandler, List<XmlTag>> myTagsGetter = new NotNullFunction<DomInvocationHandler, List<XmlTag>>() {
    @Nonnull
    public List<XmlTag> apply(final DomInvocationHandler handler) {
      XmlTag tag = handler.getXmlTag();
      if (tag == null) {
        return Collections.emptyList();
      }
      return DomImplUtil.findSubTags(tag, handler.createEvaluatedXmlName(getXmlName()), handler.getFile());
    }
  };

  public CollectionChildDescriptionImpl(final XmlName tagName, final Type type, final Collection<JavaMethod> getterMethods) {
    super(tagName, type);
    myGetterMethods = getterMethods;
  }

  @Override
  public String toString() {
    return "CollectionChildDescription:" + getXmlName();
  }

  public NotNullFunction<DomInvocationHandler, List<XmlTag>> getTagsGetter() {
    return myTagsGetter;
  }

  public DomElement addValue(@Nonnull DomElement element) {
    assert element.getGenericInfo().getCollectionChildrenDescriptions().contains(this);
    return addChild(element, getType(), Integer.MAX_VALUE);
  }

  private DomElement addChild(final DomElement element, final Type type, final int index) {
    try {
      final DomInvocationHandler handler = DomManagerImpl.getDomInvocationHandler(element);
      assert handler != null;
      return handler.addCollectionChild(this, type, index);
    }
    catch (IncorrectOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public DomElement addValue(@Nonnull DomElement element, int index) {
    return addChild(element, getType(), index);
  }

  public DomElement addValue(@Nonnull DomElement parent, Type type) {
    return addValue(parent, type, Integer.MAX_VALUE);
  }

  public final DomElement addValue(@Nonnull DomElement parent, Type type, int index) {
    return addChild(parent, type, index);
  }

  @Nullable
  public final JavaMethod getGetterMethod() {
    final Collection<JavaMethod> methods = myGetterMethods;
    return methods.isEmpty() ? null : methods.iterator().next();
  }

  @Nonnull
  public List<? extends DomElement> getValues(@Nonnull final DomElement element) {
    final DomInvocationHandler handler = DomManagerImpl.getDomInvocationHandler(element);
    if (handler != null) {
      return handler.getCollectionChildren(this, myTagsGetter);
    }
    final JavaMethod getterMethod = getGetterMethod();
    if (getterMethod == null) {
      final Collection<DomElement> collection = ModelMergerUtil.getFilteredImplementations(element);
      return ContainerUtil.concat(collection, new Function<DomElement, Collection<? extends DomElement>>() {
        public Collection<? extends DomElement> apply(final DomElement domElement) {
          final DomInvocationHandler handler = DomManagerImpl.getDomInvocationHandler(domElement);
          assert handler != null : domElement;
          return handler.getCollectionChildren(CollectionChildDescriptionImpl.this, myTagsGetter);
        }
      });
    }
    return (List<? extends DomElement>)getterMethod.invoke(element, ArrayUtil.EMPTY_OBJECT_ARRAY);
  }

  @Nonnull
  public String getCommonPresentableName(@Nonnull DomNameStrategy strategy) {
    String words = strategy.splitIntoWords(getXmlElementName());
    return StringUtil.capitalizeWords(words.endsWith("es") ? words: StringUtil.pluralize(words), true);
  }

  @Nullable
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    final JavaMethod method = getGetterMethod();
    if (method != null) {
      final T annotation = method.getAnnotation(annotationClass);
      if (annotation != null) return annotation;
    }

    final Type elemType = getType();
    return elemType instanceof AnnotatedElement ? ((AnnotatedElement)elemType).getAnnotation(annotationClass) : super.getAnnotation(annotationClass);
  }

  public List<XmlTag> getSubTags(final DomInvocationHandler handler, final XmlTag[] subTags, final XmlFile file) {
    return DomImplUtil.findSubTags(subTags, handler.createEvaluatedXmlName(getXmlName()), file);
  }

  public EvaluatedXmlName createEvaluatedXmlName(final DomInvocationHandler parent, final XmlTag childTag) {
    return parent.createEvaluatedXmlName(getXmlName());
  }
}
