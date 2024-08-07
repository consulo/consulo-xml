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
 * User: spleaner
 * Date: Jun 19, 2007
 * Time: 4:44:25 PM
 */
package com.intellij.xml.breadcrumbs;

import consulo.ide.impl.idea.ui.breadcrumbs.BreadcrumbsProvider;
import consulo.language.Language;
import consulo.xml.lang.xml.XMLLanguage;
import consulo.language.psi.PsiElement;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import consulo.annotation.access.RequiredReadAction;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class XmlLanguageBreadcrumbsInfoProvider implements BreadcrumbsProvider {
    @NonNls
    protected static final String CLASS_ATTRIBUTE_NAME = "class";
    @NonNls
    protected static final String ID_ATTRIBUTE_NAME = "id";

    @RequiredReadAction
    @Override
    public boolean acceptElement(@Nonnull final PsiElement e) {
        return e instanceof XmlTag && e.isValid();
    }

    @Override
    @Nonnull
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public String getElementInfo(@Nonnull final PsiElement e) {
        final XmlTag tag = (XmlTag)e;
        return tag.getName();
    }

    @RequiredReadAction
    @Override
    @Nullable
    public String getElementTooltip(@Nonnull final PsiElement e) {
        final XmlTag tag = (XmlTag)e;
        final StringBuilder result = new StringBuilder("&lt;");
        result.append(tag.getName());
        final XmlAttribute[] attributes = tag.getAttributes();
        for (final XmlAttribute each : attributes) {
            result.append(" ").append(each.getText());
        }

        if (tag.isEmpty()) {
            result.append("/&gt;");
        }
        else {
            result.append("&gt;...&lt;/").append(tag.getName()).append("&gt;");
        }

        return result.toString();
    }
}