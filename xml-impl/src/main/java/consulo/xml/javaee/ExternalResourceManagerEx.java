/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.xml.javaee;

import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface ExternalResourceManagerEx extends ExternalResourceManager {
  public static final String STANDARD_SCHEMAS = "/standardSchemas/";

  public enum XMLSchemaVersion {
    XMLSchema_1_0, XMLSchema_1_1
  }

  @Deprecated
  public static ExternalResourceManagerEx getInstanceEx() {
    return (ExternalResourceManagerEx) ApplicationExternalResourceManager.getInstance();
  }

  public abstract void removeResource(String url, @Nonnull Project project);

  public abstract void addResource(@NonNls String url, @NonNls String location, @Nonnull Project project);

  public abstract String[] getAvailableUrls();

  public abstract String[] getAvailableUrls(Project project);

  public abstract void clearAllResources();

  public abstract void clearAllResources(Project project);

  public abstract void addIgnoredResource(@Nonnull String url);

  public abstract void removeIgnoredResource(@Nonnull String url);

  public abstract boolean isIgnoredResource(@Nonnull String url);

  public abstract String[] getIgnoredResources();

  public abstract void addExternalResourceListener(ExternalResourceListener listener);

  public abstract void removeExternalResourceListener(ExternalResourceListener listener);

  public abstract boolean isUserResource(VirtualFile file);

  public abstract boolean isStandardResource(VirtualFile file);

  @Nullable
  public abstract String getUserResource(Project project, String url, String version);

  @Nullable
  public abstract String getStdResource(@Nonnull String url, @Nullable String version);

  @Nonnull
  public abstract String getDefaultHtmlDoctype(@Nonnull Project project);

  public abstract void setDefaultHtmlDoctype(@Nonnull String defaultHtmlDoctype, @Nonnull Project project);

  public abstract XMLSchemaVersion getXmlSchemaVersion(@Nonnull Project project);

  public abstract void setXmlSchemaVersion(XMLSchemaVersion version, @Nonnull Project project);

  public abstract String getCatalogPropertiesFile();

  public abstract void setCatalogPropertiesFile(@Nullable String filePath);

  public abstract long getModificationCount(@Nonnull Project project);

  public abstract MultiMap<String, String> getUrlsByNamespace(Project project);
}
