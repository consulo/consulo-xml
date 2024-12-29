package org.intellij.plugins.relaxNG.xml;

import com.intellij.xml.Html5SchemaProvider;
import consulo.annotation.component.ExtensionImpl;

import jakarta.annotation.Nonnull;
import java.net.URL;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class RngHtml5SchemaProvider extends Html5SchemaProvider {
  @Nonnull
  @Override
  public URL getHtmlSchemaLocation() {
    return RngHtml5SchemaProvider.class.getResource("/resources/html5-schema/html5.rnc");
  }

  @Nonnull
  @Override
  public URL getXhtmlSchemaLocation() {
    return RngHtml5SchemaProvider.class.getResource("/resources/html5-schema/xhtml5.rnc");
  }

  @Nonnull
  @Override
  public URL getCharsLocation() {
    return RngHtml5SchemaProvider.class.getResource("/resources/html5-schema/html5chars.ent");
  }
}
