// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.xml;

import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;

public interface IdeValidationHost extends Validator.ValidationHost {
  void addMessageWithFixes(PsiElement context, String message, @Nonnull ErrorType type, @Nonnull IntentionAction... fixes);
}
