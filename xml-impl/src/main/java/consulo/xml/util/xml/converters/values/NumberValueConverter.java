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

package consulo.xml.util.xml.converters.values;

import consulo.localize.LocalizeValue;
import consulo.xml.util.xml.ConvertContext;
import consulo.xml.util.xml.DomBundle;
import consulo.xml.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;

public class NumberValueConverter extends ResolvingConverter<String> {

  private final Class myNumberClass;
  private final boolean myAllowEmpty;


  public NumberValueConverter(@Nonnull final Class numberClass, final boolean allowEmpty) {
    myNumberClass = numberClass;
    myAllowEmpty = allowEmpty;
  }

  public String fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    if (s == null) return null;

    if (myAllowEmpty && s.trim().length() == 0) return s;

    return parseNumber(s, myNumberClass) == null ? null : s;
  }

  public String toString(@Nullable final String s, final ConvertContext context) {
    return null;
  }

  @Nonnull
  public LocalizeValue buildUnresolvedMessage(@Nullable final String s, final ConvertContext context) {
    if (s == null) return super.buildUnresolvedMessage(s, context);

    return LocalizeValue.localizeTODO(s.isBlank() ?
          DomBundle.message("value.converter.format.exception.empty.string", myNumberClass.getName()) :
          DomBundle.message("value.converter.format.exception", s, myNumberClass.getName()));
  }

  @Nonnull
  @Override
  public Collection<? extends String> getVariants(ConvertContext context) {
    return Collections.emptySet();
  }

  @Nullable
  public static Number parseNumber(@Nonnull String text, @Nonnull Class targetClass) {
    try {
      String trimmed = text.trim();

      if (targetClass.equals(Byte.class) || targetClass.equals(byte.class)) {
        return Byte.decode(trimmed);
      }
      else if (targetClass.equals(Short.class) || targetClass.equals(short.class)) {
        return Short.decode(trimmed);
      }
      else if (targetClass.equals(Integer.class) || targetClass.equals(int.class)) {
        return Integer.decode(trimmed);
      }
      else if (targetClass.equals(Long.class) || targetClass.equals(long.class)) {
        return Long.decode(trimmed);
      }
      else if (targetClass.equals(BigInteger.class)) {
        return decodeBigInteger(trimmed);
      }
      else if (targetClass.equals(Float.class) || targetClass.equals(float.class)) {
        return Float.valueOf(trimmed);
      }
      else if (targetClass.equals(Double.class) || targetClass.equals(double.class)) {
        return Double.valueOf(trimmed);
      }
      else if (targetClass.equals(BigDecimal.class) || targetClass.equals(Number.class)) {
        return new BigDecimal(trimmed);
      }
    } catch (NumberFormatException ex) {
      return null;
    }
    return null;
  }

  private static BigInteger decodeBigInteger(String value) {
    int radix = 10;
    int index = 0;
    boolean negative = false;

    // Handle minus sign, if present.
    if (value.startsWith("-")) {
      negative = true;
      index++;
    }

    // Handle radix specifier, if present.
    if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
      index += 2;
      radix = 16;
    }
    else if (value.startsWith("#", index)) {
      index++;
      radix = 16;
    }
    else if (value.startsWith("0", index) && value.length() > 1 + index) {
      index++;
      radix = 8;
    }

    BigInteger result = new BigInteger(value.substring(index), radix);
    return (negative ? result.negate() : result);
  }
}