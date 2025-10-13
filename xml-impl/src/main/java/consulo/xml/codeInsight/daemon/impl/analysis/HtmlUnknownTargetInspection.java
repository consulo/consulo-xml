/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.xml.util.AnchorReferenceImpl;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.xml.localize.XmlLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class HtmlUnknownTargetInspection extends XmlPathReferenceInspection {
    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return XmlLocalize.htmlInspectionsUnknownTarget();
    }

    @Nullable
    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroupDisplayName() {
        return XmlLocalize.htmlInspectionsGroupName();
    }

    @Nonnull
    @Override
    public String getShortName() {
        return "HtmlUnknownTarget";
    }

    @Override
    protected boolean isForHtml() {
        return true;
    }

    @Override
    protected boolean needToCheckRef(PsiReference reference) {
        return !(reference instanceof AnchorReferenceImpl);
    }
}
