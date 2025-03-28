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

import consulo.language.impl.ast.TreeElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiInvalidElementAccessException;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.xml.util.xml.reflect.DomFixedChildDescription;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author peter
 */
public class DomImplUtil {
  private static final Logger LOG = Logger.getInstance(DomImplUtil.class);

  private DomImplUtil() {
  }

  public static void assertValidity(DomElement element, String msg) {
    if (element instanceof DomFileElementImpl) {
      final String s = ((DomFileElementImpl) element).checkValidity();
      if (s != null) {
        throw new AssertionError(s);
      }
      return;
    }

    final DomInvocationHandler handler = DomManagerImpl.getDomInvocationHandler(element);
    assert handler != null;
    try {
      handler.assertValid();
    } catch (AssertionError e) {
      throw new AssertionError(msg + e.getMessage());
    }
  }

  public static boolean isTagValueGetter(final JavaMethod method) {
    if (!isGetter(method)) {
      return false;
    }
    if (hasTagValueAnnotation(method)) {
      return true;
    }
    if ("getValue".equals(method.getName())) {
      if (method.getAnnotation(SubTag.class) != null) return false;
      if (method.getAnnotation(SubTagList.class) != null) return false;
      if (method.getAnnotation(Convert.class) != null || method.getAnnotation(Resolve.class) != null) {
        return !ReflectionUtil.isAssignable(GenericDomValue.class, method.getReturnType());
      }
      if (ReflectionUtil.isAssignable(DomElement.class, method.getReturnType())) return false;
      return true;
    }
    return false;
  }

  private static boolean hasTagValueAnnotation(final JavaMethod method) {
    return method.getAnnotation(TagValue.class) != null;
  }

  public static boolean isGetter(final JavaMethod method) {
    @NonNls final String name = method.getName();
    final boolean isGet = name.startsWith("get");
    final boolean isIs = !isGet && name.startsWith("is");
    if (!isGet && !isIs) {
      return false;
    }
    if (method.getGenericParameterTypes().length != 0) {
      return false;
    }
    final Type returnType = method.getGenericReturnType();
    if (isGet) {
      return returnType != void.class;
    }
    return DomReflectionUtil.canHaveIsPropertyGetterPrefix(returnType);
  }


  public static boolean isTagValueSetter(final JavaMethod method) {
    boolean setter = method.getName().startsWith("set") && method.getGenericParameterTypes().length == 1 && method.getReturnType() == void.class;
    return setter && (hasTagValueAnnotation(method) || "setValue".equals(method.getName()));
  }

  @Nullable
  public static DomNameStrategy getDomNameStrategy(final Class<?> rawType, boolean isAttribute) {
    Class aClass = null;
    if (isAttribute) {
      NameStrategyForAttributes annotation = DomReflectionUtil.findAnnotationDFS(rawType, NameStrategyForAttributes.class);
      if (annotation != null) {
        aClass = annotation.value();
      }
    }
    if (aClass == null) {
      NameStrategy annotation = DomReflectionUtil.findAnnotationDFS(rawType, NameStrategy.class);
      if (annotation != null) {
        aClass = annotation.value();
      }
    }
    if (aClass != null) {
      if (HyphenNameStrategy.class.equals(aClass)) return DomNameStrategy.HYPHEN_STRATEGY;
      if (JavaNameStrategy.class.equals(aClass)) return DomNameStrategy.JAVA_STRATEGY;
      try {
        return (DomNameStrategy) aClass.newInstance();
      } catch (InstantiationException e) {
        LOG.error(e);
      } catch (IllegalAccessException e) {
        LOG.error(e);
      }
    }
    return null;
  }

  public static List<XmlTag> findSubTags(@Nonnull final XmlTag tag, final EvaluatedXmlName name, final XmlFile file) {
    if (!tag.isValid()) {
      throw new AssertionError("Invalid tag");
    }
    final XmlTag[] tags = tag.getSubTags();
    if (tags.length == 0) {
      return Collections.emptyList();
    }

    return ContainerUtil.findAll(tags, new Condition<XmlTag>() {
      public boolean value(XmlTag childTag) {
        try {
          return isNameSuitable(name, childTag.getLocalName(), childTag.getName(), childTag.getNamespace(), file);
        } catch (PsiInvalidElementAccessException e) {
          if (!childTag.isValid()) {
            LOG.error("tag.getSubTags() returned invalid, " +
                "tag=" + tag + ", " +
                "containing file: " + tag.getContainingFile() +
                "subTag.parent=" + childTag.getNode().getTreeParent());
            return false;
          }
          throw e;
        }
      }
    });
  }

  public static List<XmlTag> findSubTags(final XmlTag[] tags, final EvaluatedXmlName name, final XmlFile file) {
    if (tags.length == 0) {
      return Collections.emptyList();
    }

    return ContainerUtil.findAll(tags, new Condition<XmlTag>() {
      public boolean value(XmlTag childTag) {
        return isNameSuitable(name, childTag, file);
      }
    });
  }

  public static boolean isNameSuitable(final XmlName name, final XmlTag tag, @Nonnull final DomInvocationHandler handler, final XmlFile file) {
    return isNameSuitable(handler.createEvaluatedXmlName(name), tag, file);
  }

  private static boolean isNameSuitable(final EvaluatedXmlName evaluatedXmlName, final XmlTag tag, final XmlFile file) {
    if (!tag.isValid()) {
      TreeElement parent = ((TreeElement) tag).getTreeParent();
      throw new AssertionError("Invalid child tag of valid parent. Parent:" + (parent == null ? null : parent.getPsi().isValid()));
    }
    return isNameSuitable(evaluatedXmlName, tag.getLocalName(), tag.getName(), tag.getNamespace(), file);
  }

  public static boolean isNameSuitable(final EvaluatedXmlName evaluatedXmlName, final String localName, final String qName, final String namespace,
                                       final XmlFile file) {
    final String localName1 = evaluatedXmlName.getXmlName().getLocalName();
    return (localName1.equals(localName) || localName1.equals(qName)) && evaluatedXmlName.isNamespaceAllowed(namespace, file,
        !localName1.equals(qName));
  }

  @Nullable
  public static XmlName createXmlName(@Nonnull String name, Type type, @Nullable JavaMethod javaMethod) {
    final Class<?> aClass = getErasure(type);
    if (aClass == null) return null;
    String key = getNamespaceKey(aClass);
    if (key == null && javaMethod != null) {
      for (final Method method : javaMethod.getHierarchy()) {
        final String key1 = getNamespaceKey(method.getDeclaringClass());
        if (key1 != null) {
          return new XmlName(name, key1);
        }
      }
    }
    return new XmlName(name, key);
  }

  @Nullable
  private static Class<?> getErasure(Type type) {
    if (type instanceof Class) {
      return (Class) type;
    }
    if (type instanceof ParameterizedType) {
      return getErasure(((ParameterizedType) type).getRawType());
    }
    if (type instanceof TypeVariable) {
      for (final Type bound : ((TypeVariable) type).getBounds()) {
        final Class<?> aClass = getErasure(bound);
        if (aClass != null) {
          return aClass;
        }
      }
    }
    if (type instanceof WildcardType) {
      final WildcardType wildcardType = (WildcardType) type;
      for (final Type bound : wildcardType.getUpperBounds()) {
        final Class<?> aClass = getErasure(bound);
        if (aClass != null) {
          return aClass;
        }
      }
    }
    return null;
  }

  @Nullable
  private static String getNamespaceKey(@Nonnull Class<?> type) {
    final Namespace namespace = DomReflectionUtil.findAnnotationDFS(type, Namespace.class);
    return namespace != null ? namespace.value() : null;
  }

  @Nullable
  public static XmlName createXmlName(@Nonnull final String name, final JavaMethod method) {
    return createXmlName(name, method.getGenericReturnType(), method);
  }

  public static List<XmlTag> getCustomSubTags(final DomInvocationHandler handler, final XmlTag[] subTags, final XmlFile file) {
    if (subTags.length == 0) {
      return Collections.emptyList();
    }

    final DomGenericInfoEx info = handler.getGenericInfo();
    final Set<XmlName> usedNames = new HashSet<XmlName>();
    List<? extends DomCollectionChildDescription> collectionChildrenDescriptions = info.getCollectionChildrenDescriptions();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, size = collectionChildrenDescriptions.size(); i < size; i++) {
      DomCollectionChildDescription description = collectionChildrenDescriptions.get(i);
      usedNames.add(description.getXmlName());
    }
    List<? extends DomFixedChildDescription> fixedChildrenDescriptions = info.getFixedChildrenDescriptions();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, size = fixedChildrenDescriptions.size(); i < size; i++) {
      DomFixedChildDescription description = fixedChildrenDescriptions.get(i);
      usedNames.add(description.getXmlName());
    }
    return ContainerUtil.findAll(subTags, new Condition<XmlTag>() {
      public boolean value(final XmlTag tag) {
        if (StringUtil.isEmpty(tag.getName())) return false;

        for (final XmlName name : usedNames) {
          if (isNameSuitable(name, tag, handler, file)) {
            return false;
          }
        }
        return true;
      }
    });
  }

  static XmlFile getFile(DomElement domElement) {
    if (domElement instanceof DomFileElement) {
      return ((DomFileElement) domElement).getFile();
    }
    DomInvocationHandler handler = DomManagerImpl.getDomInvocationHandler(domElement);
    assert handler != null : domElement;
    while (handler != null && !(handler instanceof DomRootInvocationHandler) && handler.getXmlTag() == null) {
      handler = handler.getParentHandler();
    }
    if (handler instanceof DomRootInvocationHandler) {
      return ((DomRootInvocationHandler) handler).getParent().getFile();
    }
    assert handler != null;
    XmlTag tag = handler.getXmlTag();
    assert tag != null;
    while (true) {
      final PsiElement parentTag = PhysicalDomParentStrategy.getParentTagCandidate(tag);
      if (!(parentTag instanceof XmlTag)) {
        return (XmlFile) tag.getContainingFile();
      }

      tag = (XmlTag) parentTag;
    }
  }
}
