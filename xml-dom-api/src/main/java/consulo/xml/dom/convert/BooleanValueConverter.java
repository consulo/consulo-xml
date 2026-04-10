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

package consulo.xml.dom.convert;

import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import consulo.xml.dom.ConvertContext;
import consulo.xml.dom.ResolvingConverter;
import consulo.xml.dom.localize.DomLocalize;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class BooleanValueConverter extends ResolvingConverter<String> {
  private static final String BOOLEAN = "boolean";

  private static final String[] VARIANTS = {"false", "true"};

  private final boolean myAllowEmpty;

  public static BooleanValueConverter getInstance(final boolean allowEmpty) {
     return new BooleanValueConverter(allowEmpty);
  }

  public BooleanValueConverter() {
    this(false);
  }

  public BooleanValueConverter(final boolean allowEmpty) {
    myAllowEmpty = allowEmpty;
  }

  public String[] getAllValues() {
    final String[] strings = ArrayUtil.mergeArrays(getTrueValues(), getFalseValues());

    Arrays.sort(strings);

    return strings;
  }

  public String[] getTrueValues() {
    return new String[] {"true"};
  }

  public String[] getFalseValues() {
    return new String[] {"false"};
  }

  public boolean isTrue(String s) {
    return Arrays.binarySearch(getTrueValues(), s) >= 0;
  }

  public String fromString(@Nullable final String stringValue, final ConvertContext context) {
    if (stringValue != null && ((myAllowEmpty && stringValue.trim().length() == 0) || Arrays.binarySearch(getAllValues(), stringValue) >= 0)) {
      return stringValue;
    }
    return null;
  }

  public String toString(@Nullable final String s, final ConvertContext context) {
    return s;
  }

  public Collection<? extends String> getVariants(final ConvertContext context) {
    return Arrays.asList(VARIANTS);
  }

  public LocalizeValue buildUnresolvedMessage(@Nullable final String s, final ConvertContext context) {
    return DomLocalize.valueConverterFormatException(s, BOOLEAN);
  }
}
