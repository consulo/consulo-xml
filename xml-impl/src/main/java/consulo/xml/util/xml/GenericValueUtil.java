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

import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.util.collection.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
public class GenericValueUtil {
  private GenericValueUtil() {
  }

  public static NullableFunction<GenericValue, String> STRING_VALUE = genericValue -> genericValue.getStringValue();
  public static NullableFunction<GenericValue, Object> OBJECT_VALUE = genericValue -> genericValue.getValue();


  public static boolean containsString(final List<? extends GenericValue<?>> list, String value) {
    for (GenericValue<?> o : list) {
      if (Comparing.equal(value, o.getStringValue())) return true;
    }
    return false;
  }

  public static <T> boolean containsValue(final List<? extends GenericValue<? extends T>> list, T value) {
    for (GenericValue<? extends T> o : list) {
      if (Comparing.equal(value, o.getValue())) return true;
    }
    return false;
  }

  @Nonnull
  public static <T> Collection<T> getValueCollection(final Collection<? extends GenericValue<? extends T>> collection, Collection<T> result) {
    for (GenericValue<? extends T> o : collection) {
      ContainerUtil.addIfNotNull(result, o.getValue());
    }
    return result;
  }

  @Nonnull
  public static Collection<String> getStringCollection(final Collection<? extends GenericValue> collection, Collection<String> result) {
    for (GenericValue o : collection) {
      ContainerUtil.addIfNotNull(result, o.getStringValue());
    }
    return result;
  }

  @Nonnull
  public static Collection<String> getClassStringCollection(final Collection<? extends GenericValue> collection, Collection<String> result) {
    for (GenericValue o : collection) {
      final String value = o.getStringValue();
      if (value != null) {
        result.add(value.replace('$', '.'));
      }
    }
    return result;
  }

}
