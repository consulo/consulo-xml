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
package consulo.xml.util.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processor;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.reflect.ReflectionUtil;

/**
 * @author peter
 */
public class ModelMergerUtil {

  @Nullable
  public static <T> T getFirstImplementation(final T t) {
    T cur = t;
    while (cur instanceof MergedObject) {
      final List<T> implementations = ((MergedObject<T>)cur).getImplementations();
      cur = implementations.isEmpty()? null : implementations.get(0);
    }
    return cur;
  }

  @Nullable
  public static <T, V> V getImplementation(final Class<V> clazz, final Collection<T> elements) {
    for (final T element : elements) {
      final V implementation = getImplementation(element, clazz);
      if (implementation != null) {
        return implementation;
      }
    }
    return null;
  }

  @Nullable
  public static <T, V> V getImplementation(final Class<V> clazz, final T... elements) {
    return getImplementation(clazz, Arrays.asList(elements));
  }

  @Nullable
  public static <T, V> V getImplementation(final T element, final Class<V> clazz) {
    if (element == null) return null;
    CommonProcessors.FindFirstProcessor<T> processor = new CommonProcessors.FindFirstProcessor<T>() {
      public boolean process(final T t) {
        return !ReflectionUtil.isAssignable(clazz, t.getClass()) || super.process(t);
      }
    };
    new ImplementationProcessor<T>(processor, true).process(element);
    return (V)processor.getFoundValue();
  }

  @Nonnull
  public static <T, V> Collection<V> getImplementations(final T element, final Class<V> clazz) {
    if (element == null) return Collections.emptyList();
    CommonProcessors.CollectProcessor<T> processor = new CommonProcessors.CollectProcessor<T>() {
      public boolean process(final T t) {
        return !ReflectionUtil.isAssignable(clazz, t.getClass()) || super.process(t);
      }
    };
    new ImplementationProcessor<T>(processor, true).process(element);
    return (Collection<V>)processor.getResults();
  }

  @Nonnull
  public static <T> List<T> getImplementations(T element) {
    if (element instanceof MergedObject) {
      final MergedObject<T> mergedObject = (MergedObject<T>)element;
      return mergedObject.getImplementations();
    }
    else if (element != null) {
      return Collections.singletonList(element);
    }
    else {
      return Collections.emptyList();
    }
  }

  @Nonnull
  public static <T> List<T> getFilteredImplementations(final T element) {
    if (element == null) return Collections.emptyList();
    final CommonProcessors.CollectProcessor<T> processor = new CommonProcessors.CollectProcessor<T>(new ArrayList<T>());
    new ImplementationProcessor<T>(processor, false).process(element);
    return (List<T>)processor.getResults();
  }

  @Nonnull
  public static <T> Processor<T> createFilteringProcessor(final Processor<T> processor) {
    return new ImplementationProcessor<T>(processor, false);
  }

  public static class ImplementationProcessor<T> implements Processor<T> {
    private final Processor<T> myProcessor;
    private final boolean myProcessMerged;

    public ImplementationProcessor(Processor<T> processor, final boolean processMerged) {
      myProcessor = processor;
      myProcessMerged = processMerged;
    }

    public boolean process(final T t) {
      final boolean merged = t instanceof MergedObject;
      if ((!merged || myProcessMerged) && !myProcessor.process(t)) {
        return false;
      }
      if (merged && !ContainerUtil.process(((MergedObject<T>)t).getImplementations(), this)) {
        return false;
      }
      return true;
    }
  }
}
