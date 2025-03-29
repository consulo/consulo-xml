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
import consulo.localize.LocalizeValue;
import consulo.util.lang.lazy.LazyValue;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.xml.XmlTag;

@ExtensionImpl
public class XmlErrorQuickFixProvider implements ErrorQuickFixProvider {
    private static final LazyValue<LocalizeKey> unescapedAmpersandKey = LazyValue.notNull(() -> {
        return XmlErrorLocalize.unescapedAmpersandOrNonterminatedCharacterEntityReference().getKey().get();
    });

    @Override
    @RequiredReadAction
    public void registerErrorQuickFix(final PsiErrorElement element, final HighlightInfo.Builder builder) {
        if (PsiTreeUtil.getParentOfType(element, XmlTag.class) != null) {
            registerXmlErrorQuickFix(element, builder);
        }
    }

    private static void registerXmlErrorQuickFix(final PsiErrorElement element, final HighlightInfo.Builder builder) {
        LocalizeValue errorText = element.getErrorDescriptionValue();

        if (errorText.getKey().isPresent() && errorText.getKey().get().equals(unescapedAmpersandKey.get())) {
            builder.registerFix(new UnescapeAction(element), null, null, null, null);
        }
    }
}
