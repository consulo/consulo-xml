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

package com.intellij.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.DumbService;
import consulo.util.collection.ContainerUtil;
import consulo.xml.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class XmlSchemaProvider {
    public static final ExtensionPointName<XmlSchemaProvider> EP_NAME = ExtensionPointName.create(XmlSchemaProvider.class);

    @Nullable
    public static XmlFile findSchema(@Nonnull @NonNls String namespace, @Nullable Module module, @Nonnull PsiFile file) {
      if (file.getProject().isDefault()) {
        return null;
      }
        final boolean dumb = DumbService.getInstance(file.getProject()).isDumb();
        for (XmlSchemaProvider provider : EP_NAME.getExtensionList()) {
            if (dumb && !DumbService.isDumbAware(provider)) {
                continue;
            }

            if (file instanceof XmlFile && !provider.isAvailable((XmlFile)file)) {
                continue;
            }
            final XmlFile schema = provider.getSchema(namespace, module, file);
            if (schema != null) {
                return schema;
            }
        }
        return null;
    }

    @Nullable
    public static XmlFile findSchema(@Nonnull String namespace, @Nonnull PsiFile baseFile) {
        final PsiDirectory directory = baseFile.getParent();
        final Module module = ModuleUtilCore.findModuleForPsiElement(directory == null ? baseFile : directory);
        return findSchema(namespace, module, baseFile);
    }

    /**
     * @see #getAvailableProviders(XmlFile)
     */
    @Deprecated
    @Nullable
    public static XmlSchemaProvider getAvailableProvider(@Nonnull final XmlFile file) {
        for (XmlSchemaProvider provider : EP_NAME.getExtensionList()) {
            if (provider.isAvailable(file)) {
                return provider;
            }
        }
        return null;
    }

    public static List<XmlSchemaProvider> getAvailableProviders(@Nonnull final XmlFile file) {
        return ContainerUtil.findAll(EP_NAME.getExtensionList(), it -> it.isAvailable(file));
    }

    @Nullable
    public abstract XmlFile getSchema(@Nonnull @NonNls String url, @Nullable Module module, @Nonnull final PsiFile baseFile);


    public boolean isAvailable(@Nonnull final XmlFile file) {
        return false;
    }

    /**
     * Provides specific namespaces for given xml file.
     *
     * @param file    an xml or jsp file.
     * @param tagName optional
     * @return available namespace uris, or <code>null</code> if the provider did not recognize the file.
     */
    @Nonnull
    public Set<String> getAvailableNamespaces(@Nonnull final XmlFile file, @Nullable final String tagName) {
        return Collections.emptySet();
    }

    @Nullable
    public String getDefaultPrefix(@Nonnull @NonNls String namespace, @Nonnull final XmlFile context) {
        return null;
    }

    @Nullable
    public Set<String> getLocations(@Nonnull @NonNls String namespace, @Nonnull final XmlFile context) {
        return null;
    }
}
