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

package consulo.xml.util.xml.ui.actions;

import java.lang.reflect.Type;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.application.Result;
import consulo.ui.ex.action.AnAction;
import consulo.application.ApplicationBundle;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomManager;
import consulo.xml.util.xml.DomUtil;
import consulo.xml.util.xml.StableElement;
import consulo.xml.util.xml.TypeChooser;
import consulo.xml.util.xml.reflect.DomCollectionChildDescription;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.language.util.IncorrectOperationException;

/**
 * User: Sergey.Vasiliev
 * Date: Mar 1, 2006
 */
public abstract class DefaultAddAction<T extends DomElement> extends AnAction {

  public DefaultAddAction() {
    super(ApplicationBundle.message("action.add"));
  }

  public DefaultAddAction(String text) {
    super(text);
  }

  public DefaultAddAction(String text, String description, Image icon) {
    super(text, description, icon);
  }


  protected Type getElementType() {
    return getDomCollectionChildDescription().getType();
  }

  protected void tuneNewValue(T t) {
  }

  protected abstract DomCollectionChildDescription getDomCollectionChildDescription();

  protected abstract DomElement getParentDomElement();

  protected void afterAddition(@Nonnull T newElement) {
  }

  public final void actionPerformed(final AnActionEvent e) {
    final T result = performElementAddition();
    if (result != null) {
      afterAddition(result);
    }
  }

  @Nullable
  protected T performElementAddition() {
    final DomElement parent = getParentDomElement();
    final DomManager domManager = parent.getManager();
    final TypeChooser[] oldChoosers = new TypeChooser[]{null};
    final Type[] aClass = new Type[]{null};
    final StableElement<T> result = new WriteCommandAction<StableElement<T>>(domManager.getProject(), DomUtil.getFile(parent)) {
      protected void run(Result<StableElement<T>> result) throws Throwable {
        final DomElement parentDomElement = getParentDomElement();
        final T t = (T)getDomCollectionChildDescription().addValue(parentDomElement, getElementType());
        tuneNewValue(t);
        aClass[0] = parent.getGenericInfo().getCollectionChildDescription(t.getXmlElementName()).getType();
        oldChoosers[0] = domManager.getTypeChooserManager().getTypeChooser(aClass[0]);
        final SmartPsiElementPointer pointer =
          SmartPointerManager.getInstance(getProject()).createSmartPsiElementPointer(t.getXmlTag());
        domManager.getTypeChooserManager().registerTypeChooser(aClass[0], new TypeChooser() {
          public Type chooseType(final XmlTag tag) {
            if (tag == pointer.getElement()) {
              return getElementType();
            }
            return oldChoosers[0].chooseType(tag);
          }

          public void distinguishTag(final XmlTag tag, final Type aClass) throws IncorrectOperationException {
            oldChoosers[0].distinguishTag(tag, aClass);
          }

          public Type[] getChooserTypes() {
            return oldChoosers[0].getChooserTypes();
          }
        });
        result.setResult((StableElement<T>)t.createStableCopy());
      }
    }.execute().getResultObject();
    if (result != null) {
      domManager.getTypeChooserManager().registerTypeChooser(aClass[0], oldChoosers[0]);
      return result.getWrappedElement();
    }
    return null;
  }
}