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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlComment;
import consulo.xml.language.psi.XmlRecursiveElementVisitor;
import consulo.language.psi.resolve.PsiElementProcessor;
import com.intellij.xml.util.XmlDeclareIdInCommentAction;
import consulo.util.collection.ArrayUtil;

import consulo.xml.language.psi.XmlTag;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author peter
 */
public class IdRefReference extends BasicAttributeValueReference {
  private final boolean myIdAttrsOnly;

  public IdRefReference(PsiElement element, int offset, boolean idAttrsOnly) {
    super(element, offset);
    myIdAttrsOnly = idAttrsOnly;
  }

  public IdRefReference(PsiElement element) {
    super(element);
    myIdAttrsOnly = false;
  }

  @Nullable
  protected PsiElement getIdValueElement(PsiElement element) {
    if (element instanceof XmlTag tag) {
      XmlAttribute attribute = tag.getAttribute(IdReferenceProvider.ID_ATTR_NAME, null);
      if (!myIdAttrsOnly) {
        if (attribute == null) {
          attribute = tag.getAttribute(IdReferenceProvider.NAME_ATTR_NAME, null);
        }
        if (attribute == null) {
          attribute = tag.getAttribute(IdReferenceProvider.STYLE_ID_ATTR_NAME, null);
        }
      }
      return attribute != null ? attribute.getValueElement() : null;
    }
    else {
      return element;
    }
  }

  @Nullable
  @RequiredReadAction
  protected String getIdValue(PsiElement element) {
    if (element instanceof XmlTag tag) {
      String s = tag.getAttributeValue(IdReferenceProvider.ID_ATTR_NAME);
      if (!myIdAttrsOnly) {
        if (s == null) s = tag.getAttributeValue(IdReferenceProvider.NAME_ATTR_NAME);
        if (s == null) s = tag.getAttributeValue(IdReferenceProvider.STYLE_ID_ATTR_NAME);
      }
      return s;
    } else if (element instanceof PsiComment comment) {
      return getImplicitIdValue(comment);
    }

    return null;
  }

  protected static boolean isAcceptableTagType(XmlTag subTag) {
    return subTag.getAttributeValue(IdReferenceProvider.ID_ATTR_NAME) != null
      || subTag.getAttributeValue(IdReferenceProvider.FOR_ATTR_NAME) != null
      || (subTag.getAttributeValue(IdReferenceProvider.NAME_ATTR_NAME) != null && !subTag.getName().contains(".directive"));
  }

  private static final FileBasedUserDataCache<List<PsiElement>> CACHED_IDS_CACHE = new FileBasedUserDataCache<>("my.ids.cached.value") {
    @Override
    protected List<PsiElement> doCompute(PsiFile file) {
      final List<PsiElement> result = new ArrayList<>();

      file.accept(new XmlRecursiveElementVisitor(true) {
        @Override
        public void visitXmlTag(XmlTag tag) {
          if (isAcceptableTagType(tag)) result.add(tag);
          super.visitXmlTag(tag);
        }

        @Override
        @RequiredReadAction
        public void visitComment(PsiComment comment) {
          if (isDeclarationComment(comment)) result.add(comment);

          super.visitComment(comment);
        }

        @Override
        @RequiredReadAction
        public void visitXmlComment(XmlComment comment) {
          if (isDeclarationComment(comment)) result.add(comment);

          super.visitComment(comment);
        }
      });
      return result;
    }
  };

  @RequiredReadAction
  private static boolean isDeclarationComment(PsiComment comment) {
    return comment.getText().contains("@declare id=");
  }

  @Nullable
  @RequiredReadAction
  private static String getImplicitIdValue(PsiComment comment) {
    return XmlDeclareIdInCommentAction.getImplicitlyDeclaredId(comment);
  }

  @RequiredReadAction
  private void process(PsiElementProcessor<PsiElement> processor) {
    PsiFile psiFile = getElement().getContainingFile();
    process(processor, psiFile);
  }

  @RequiredReadAction
  public static void process(PsiElementProcessor<PsiElement> processor, PsiFile file) {
    for (PsiElement e : CACHED_IDS_CACHE.compute(file)) {
      if (!processor.execute(e)) return;
    }
  }

  @Nullable
  @Override
  @RequiredReadAction
  public PsiElement resolve() {
    final PsiElement[] result = new PsiElement[1];
    process(new PsiElementProcessor<>() {
      String canonicalText = getCanonicalText();

      @Override
      @RequiredReadAction
      public boolean execute(PsiElement element) {
        String idValue = getIdValue(element);
        if (idValue != null && idValue.equals(canonicalText)) {
          result[0] = getIdValueElement(element);
          return false;
        }
        return true;
      }
    });

    return result[0];
  }

  @Override
  @RequiredReadAction
  public Object[] getVariants() {
    List<String> result = new LinkedList<>();

    process(element -> {
      String value = getIdValue(element);
      if (value != null) {
        result.add(value);
      }
      return true;
    });

    return ArrayUtil.toObjectArray(result);
  }

  @Override
  @RequiredReadAction
  public boolean isSoft() {
    return false;
  }
}
