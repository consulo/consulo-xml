package com.intellij.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.logging.Logger;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.javaee.ExternalResourceManagerEx;

import jakarta.annotation.Nonnull;
import java.net.URL;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class Html5SchemaProvider {
    private static final Logger LOG = Logger.getInstance(Html5SchemaProvider.class);

    public static final ExtensionPointName<Html5SchemaProvider> EP_NAME = ExtensionPointName.create(Html5SchemaProvider.class);

    private static String HTML5_SCHEMA_LOCATION;
    private static String XHTML5_SCHEMA_LOCATION;
    private static String CHARS_DTD_LOCATION;

    private static boolean ourInitialized;

    public static String getHtml5SchemaLocation() {
        ensureInitialized();
        return HTML5_SCHEMA_LOCATION;
    }

    public static String getXhtml5SchemaLocation() {
        ensureInitialized();
        return XHTML5_SCHEMA_LOCATION;
    }

    public static String getCharsDtdLocation() {
        ensureInitialized();
        return CHARS_DTD_LOCATION;
    }

    private synchronized static void ensureInitialized() {
        if (ourInitialized) {
            return;
        }
        ourInitialized = true;

        final List<Html5SchemaProvider> providers = EP_NAME.getExtensionList();
        final URL htmlSchemaLocationURL;
        final URL xhtmlSchemaLocationURL;
        final URL dtdCharsLocationURL;

        if (providers.size() > 1) {
            LOG.error("More than one HTML5 schema providers found: " + providers);
        }

        if (providers.size() > 0) {
            htmlSchemaLocationURL = providers.get(0).getHtmlSchemaLocation();
            xhtmlSchemaLocationURL = providers.get(0).getXhtmlSchemaLocation();
            dtdCharsLocationURL = providers.get(0).getCharsLocation();
        }
        else {
            LOG.info("RelaxNG based schema for HTML5 is not supported. Old XSD schema will be used");
            htmlSchemaLocationURL =
                Html5SchemaProvider.class.getResource(ExternalResourceManagerEx.STANDARD_SCHEMAS + "html5/xhtml5.xsd");
            xhtmlSchemaLocationURL = htmlSchemaLocationURL;
            dtdCharsLocationURL = htmlSchemaLocationURL;
        }

        HTML5_SCHEMA_LOCATION =
            VirtualFileUtil.urlToPath(VfsUtilCore.fixURLforIDEA(URLUtil.unescapePercentSequences(htmlSchemaLocationURL.toExternalForm())));
        LOG.info("HTML5_SCHEMA_LOCATION = " + getHtml5SchemaLocation());

        XHTML5_SCHEMA_LOCATION =
            VirtualFileUtil.urlToPath(VfsUtilCore.fixURLforIDEA(URLUtil.unescapePercentSequences(xhtmlSchemaLocationURL.toExternalForm())));
        LOG.info("XHTML5_SCHEMA_LOCATION = " + getXhtml5SchemaLocation());

        CHARS_DTD_LOCATION =
            VirtualFileUtil.urlToPath(VfsUtilCore.fixURLforIDEA(URLUtil.unescapePercentSequences(dtdCharsLocationURL.toExternalForm())));
        LOG.info("CHARS_DTD_LOCATION = " + getCharsDtdLocation());
    }

    @Nonnull
    public abstract URL getHtmlSchemaLocation();

    @Nonnull
    public abstract URL getXhtmlSchemaLocation();

    @Nonnull
    public abstract URL getCharsLocation();
}
