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

import org.jspecify.annotations.Nullable;

public interface ExternalResourceManagerEx extends ExternalResourceManager {
  public static final String STANDARD_SCHEMAS = "/standardSchemas/";

  public enum XMLSchemaVersion {
    XMLSchema_1_0, XMLSchema_1_1
  }

  @Deprecated
  public static ExternalResourceManagerEx getInstanceEx() {
    return (ExternalResourceManagerEx) ApplicationExternalResourceManager.getInstance();
  }

  public abstract void removeResource(String url, Project project);

  public abstract void addResource(String url, String location, Project project);

  public abstract String[] getAvailableUrls();

  public abstract String[] getAvailableUrls(Project project);

  public abstract void clearAllResources();

  public abstract void clearAllResources(Project project);

  public abstract void addIgnoredResource(String url);

  public abstract void removeIgnoredResource(String url);

  public abstract boolean isIgnoredResource(String url);

  public abstract String[] getIgnoredResources();

  public abstract void addExternalResourceListener(ExternalResourceListener listener);

  public abstract void removeExternalResourceListener(ExternalResourceListener listener);

  public abstract boolean isUserResource(VirtualFile file);

  public abstract boolean isStandardResource(VirtualFile file);

  @Nullable
  public abstract String getUserResource(Project project, String url, String version);

  @Nullable
  public abstract String getStdResource(String url, @Nullable String version);

  public abstract String getDefaultHtmlDoctype(Project project);

  public abstract void setDefaultHtmlDoctype(String defaultHtmlDoctype, Project project);

  public abstract XMLSchemaVersion getXmlSchemaVersion(Project project);

  public abstract void setXmlSchemaVersion(XMLSchemaVersion version, Project project);

  public abstract String getCatalogPropertiesFile();

  public abstract void setCatalogPropertiesFile(@Nullable String filePath);

  public abstract long getModificationCount(Project project);

  public abstract MultiMap<String, String> getUrlsByNamespace(Project project);
}
