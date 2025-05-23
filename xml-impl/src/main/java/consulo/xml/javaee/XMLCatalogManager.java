/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.PropertyResourceBundle;

/**
 * @author Dmitry Avdeev
 *         Date: 7/20/12
 */
public class XMLCatalogManager {

  private final static Logger LOG = Logger.getInstance(XMLCatalogManager.class);

  private static Field ourResources;
  private static Field ourPropertyFileUri;

  static {
    try {
      ourResources = CatalogManager.class.getDeclaredField("resources");
      ourResources.setAccessible(true);
      ourPropertyFileUri = CatalogManager.class.getDeclaredField("propertyFileURI");
      ourPropertyFileUri.setAccessible(true);
    }
    catch (NoSuchFieldException e) {
      LOG.error(e);
    }
  }

  private final CatalogManager myManager = new CatalogManager();

  public XMLCatalogManager(@Nonnull String propertiesFilePath) {

    File file = new File(propertiesFilePath);
    try {
      String s = Files.readString(file.toPath());
      PropertyResourceBundle bundle = new PropertyResourceBundle(new StringReader(s));
      ourResources.set(myManager, bundle);
      ourPropertyFileUri.set(myManager, file.toURI().toURL());
    }
    catch (IllegalAccessException e) {
      LOG.error(e);
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  @Nullable
  public String resolve(String uri) {
    try {
      Catalog catalog = myManager.getCatalog();
      if (catalog == null) return null;
      String resolved = catalog.resolveSystem(uri);
      return resolved == null ? catalog.resolvePublic(uri, null) : resolved;
    }
    catch (IOException e) {
      LOG.warn(e);
      return null;
    }
  }

  @TestOnly
  public CatalogManager getManager() {
    return myManager;
  }
}
