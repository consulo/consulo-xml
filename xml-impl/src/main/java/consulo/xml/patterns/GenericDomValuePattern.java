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
package consulo.xml.patterns;

import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.GenericDomValue;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.InitialPatternCondition;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.StandardPatterns;
import consulo.language.util.ProcessingContext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class GenericDomValuePattern<T> extends DomElementPattern<GenericDomValue<T>, GenericDomValuePattern<T>>{
  private static final InitialPatternCondition CONDITION = new InitialPatternCondition(GenericDomValue.class) {
    public boolean accepts(@Nullable final Object o, final ProcessingContext context) {
      return o instanceof GenericDomValue;
    }
  };

  protected GenericDomValuePattern() {
    super(CONDITION);
  }

  protected GenericDomValuePattern(final Class<T> aClass) {
    super(new InitialPatternCondition(aClass) {
      public boolean accepts(@Nullable final Object o, final ProcessingContext context) {
        return o instanceof GenericDomValue && aClass.equals(DomUtil.getGenericValueParameter(((GenericDomValue)o).getDomElementType()));
      }

    });
  }

  public GenericDomValuePattern<T> withStringValue(final ElementPattern<String> pattern) {
    return with(new PatternCondition<GenericDomValue<T>>("withStringValue") {
      public boolean accepts(@Nonnull final GenericDomValue<T> genericDomValue, final ProcessingContext context) {
        return pattern.getCondition().accepts(genericDomValue.getStringValue(), context);
      }

    });
  }

  public GenericDomValuePattern<T> withValue(@Nonnull final T value) {
    return withValue(StandardPatterns.object(value));
  }

  public GenericDomValuePattern<T> withValue(final ElementPattern<?> pattern) {
    return with(new PatternCondition<GenericDomValue<T>>("withValue") {
      public boolean accepts(@Nonnull final GenericDomValue<T> genericDomValue, final ProcessingContext context) {
        return pattern.getCondition().accepts(genericDomValue.getValue(), context);
      }
    });
  }
}
