// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.xml.editor;

import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;

import consulo.localize.LocalizeValue;
import consulo.xml.language.Validator;

public interface IdeValidationHost extends Validator.ValidationHost {
    void addMessageWithFixes(
        PsiElement context,
        LocalizeValue message,
        ErrorType type,
        IntentionAction... fixes
    );
}
