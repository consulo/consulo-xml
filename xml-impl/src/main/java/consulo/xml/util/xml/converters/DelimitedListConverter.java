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

/*
 * Created by IntelliJ IDEA.
 * User: Sergey.Vasiliev
 * Date: Nov 13, 2006
 * Time: 4:37:22 PM
 */
package consulo.xml.util.xml.converters;

import consulo.document.util.TextRange;
import consulo.language.psi.EmptyResolveMessageProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.xml.util.xml.ConvertContext;
import consulo.xml.util.xml.CustomReferenceConverter;
import consulo.xml.util.xml.GenericDomValue;
import consulo.xml.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public abstract class DelimitedListConverter<T> extends ResolvingConverter<List<T>> implements CustomReferenceConverter<List<T>> {

  protected final static Object[] EMPTY_ARRAY = ArrayUtil.EMPTY_OBJECT_ARRAY;

  private final String myDelimiters;

  public DelimitedListConverter(@NonNls @Nonnull String delimiters) {

    myDelimiters = delimiters;
  }

  @Nullable
  protected abstract T convertString(final @Nullable String string, final ConvertContext context);

  @Nullable
  protected abstract String toString(@Nullable final T t);


  protected abstract Object[] getReferenceVariants(final ConvertContext context, GenericDomValue<List<T>> genericDomValue);

  @Nullable
  protected abstract PsiElement resolveReference(@Nullable final T t, final ConvertContext context);

  protected abstract LocalizeValue buildUnresolvedMessageInner(String value);

  @Nonnull
  public Collection<? extends List<T>> getVariants(final ConvertContext context) {
    return Collections.emptyList();
  }

  public static <T> void filterVariants(List<T> variants, GenericDomValue<List<T>> genericDomValue) {
    final List<T> list = genericDomValue.getValue();
    if (list != null) {
      for (Iterator<T> i = variants.iterator(); i.hasNext(); ) {
        final T variant = i.next();
        for (T existing : list) {
          if (existing.equals(variant)) {
            i.remove();
            break;
          }
        }
      }
    }
  }

  protected char getDefaultDelimiter() {
    return myDelimiters.charAt(0);
  }

  public List<T> fromString(@Nullable final String str, final ConvertContext context) {
    if (str == null) {
      return null;
    }
    List<T> values = new ArrayList<T>();

    for (String s : StringUtil.tokenize(str, myDelimiters)) {
      final T t = convertString(s.trim(), context);
      if (t != null) {
        values.add(t);
      }
    }
    return values;
  }

  public String toString(final List<T> ts, final ConvertContext context) {
    final StringBuilder buffer = new StringBuilder();
    final char delimiter = getDefaultDelimiter();
    for (T t : ts) {
      final String s = toString(t);
      if (s != null) {
        if (buffer.length() != 0) {
          buffer.append(delimiter);
        }
        buffer.append(s);
      }
    }
    return buffer.toString();
  }

  @Nonnull
  public PsiReference[] createReferences(final GenericDomValue<List<T>> genericDomValue,
                                         final PsiElement element,
                                         final ConvertContext context) {

    final String text = genericDomValue.getRawText();
    if (text == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    final ArrayList<PsiReference> references = new ArrayList<PsiReference>();
    new DelimitedListProcessor(myDelimiters) {
      protected void processToken(final int start, final int end, final boolean delimitersOnly) {
        references.add(createPsiReference(element, start + 1, end + 1, context, genericDomValue, delimitersOnly));
      }
    }.processText(text);
    return references.toArray(new PsiReference[references.size()]);
  }

  @Nonnull
  protected PsiReference createPsiReference(final PsiElement element,
                                            int start,
                                            int end,
                                            final ConvertContext context,
                                            final GenericDomValue<List<T>> genericDomValue,
                                            final boolean delimitersOnly) {

    return new MyPsiReference(element, new TextRange(start, end), context, genericDomValue, delimitersOnly);
  }

  @Override
  public String toString() {
    return super.toString() + " delimiters: " + myDelimiters;
  }

  protected class MyPsiReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {
    protected final ConvertContext myContext;
    protected final GenericDomValue<List<T>> myGenericDomValue;
    private final boolean myDelimitersOnly;

    public MyPsiReference(final PsiElement element,
                          final TextRange range,
                          final ConvertContext context,
                          final GenericDomValue<List<T>> genericDomValue,
                          final boolean delimitersOnly) {
      this(element, range, context, genericDomValue, true, delimitersOnly);
    }

    public MyPsiReference(final PsiElement element,
                          final TextRange range,
                          final ConvertContext context,
                          final GenericDomValue<List<T>> genericDomValue,
                          boolean soft,
                          final boolean delimitersOnly) {
      super(element, range, soft);
      myContext = context;
      myGenericDomValue = genericDomValue;
      myDelimitersOnly = delimitersOnly;
    }

    @Nullable
    public PsiElement resolve() {
      if (myDelimitersOnly) {
        return getElement();
      }
      final String value = getValue();
      return resolveReference(convertString(value, myContext), myContext);
    }

    @Nonnull
    public Object[] getVariants() {
      return getReferenceVariants(myContext, myGenericDomValue);
    }

    @Override
    public PsiElement handleElementRename(final String newElementName) throws consulo.language.util.IncorrectOperationException {
      final Ref<IncorrectOperationException> ref = new Ref<IncorrectOperationException>();
      PsiElement element = referenceHandleElementRename(this, newElementName, getSuperElementRenameFunction(ref));
      if (!ref.isNull()) {
        throw ref.get();
      }

      return element;
    }

    @Override
    public PsiElement bindToElement(@Nonnull final PsiElement element) throws consulo.language.util.IncorrectOperationException {
      final Ref<IncorrectOperationException> ref = new Ref<IncorrectOperationException>();
      PsiElement bindElement =
        referenceBindToElement(this, element, getSuperBindToElementFunction(ref), getSuperElementRenameFunction(ref));
      if (!ref.isNull()) {
        throw ref.get();
      }

      return bindElement;
    }

    @Override
    public String toString() {
      return super.toString() + " converter: " + DelimitedListConverter.this;
    }

    private Function<PsiElement, PsiElement> getSuperBindToElementFunction(final Ref<IncorrectOperationException> ref) {
      return s -> {
        try {
          return MyPsiReference.super.bindToElement(s);
        }
        catch (IncorrectOperationException e) {
          ref.set(e);
        }
        return null;
      };
    }

    private Function<String, PsiElement> getSuperElementRenameFunction(final Ref<consulo.language.util.IncorrectOperationException> ref) {
      return new Function<String, PsiElement>() {
        public PsiElement apply(final String s) {
          try {
            return MyPsiReference.super.handleElementRename(s);
          }
          catch (consulo.language.util.IncorrectOperationException e) {
            ref.set(e);
          }
          return null;
        }
      };
    }

    @Nonnull
    @Override
    public LocalizeValue buildUnresolvedMessage(@Nonnull String s) {
      return buildUnresolvedMessageInner(getValue());
    }
  }

  protected PsiElement referenceBindToElement(final PsiReference psiReference, final PsiElement element,
                                              final Function<PsiElement, PsiElement> superBindToElementFunction,
                                              final Function<String, PsiElement> superElementRenameFunction)
    throws consulo.language.util.IncorrectOperationException {
    return superBindToElementFunction.apply(element);
  }

  protected PsiElement referenceHandleElementRename(final PsiReference psiReference,
                                                    final String newName,
                                                    final Function<String, PsiElement> superHandleElementRename)
    throws consulo.language.util.IncorrectOperationException {

    return superHandleElementRename.apply(newName);
  }

}
