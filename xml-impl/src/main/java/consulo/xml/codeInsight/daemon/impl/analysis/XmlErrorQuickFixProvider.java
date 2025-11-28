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
package consulo.xml.codeInsight.daemon.impl.analysis;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.intention.ErrorQuickFixProvider;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeKey;
import consulo.util.lang.lazy.LazyValue;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlTag;

import java.util.Optional;

@ExtensionImpl
public class XmlErrorQuickFixProvider implements ErrorQuickFixProvider {
    private static final LazyValue<LocalizeKey> UNESCAPED_AMPERSAND_KEY =
        LazyValue.notNull(() -> XmlErrorLocalize.unescapedAmpersandOrNonterminatedCharacterEntityReference().getKey().get());

    @Override
    @RequiredReadAction
    public void registerErrorQuickFix(PsiErrorElement element, HighlightInfo.Builder builder) {
        if (PsiTreeUtil.getParentOfType(element, XmlTag.class) != null) {
            registerXmlErrorQuickFix(element, builder);
        }
    }

    private static void registerXmlErrorQuickFix(PsiErrorElement element, HighlightInfo.Builder builder) {
        Optional<LocalizeKey> errorKey = element.getErrorDescriptionValue().getKey();
        if (errorKey.isPresent() && errorKey.get().equals(UNESCAPED_AMPERSAND_KEY.get())) {
            builder.registerFix(new UnescapeAction(element));
        }
    }
}
