package consulo.xml.language;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public class XmlSharedUtil {
    public static final String XML_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
    public static final String XML_SCHEMA_URI2 = "http://www.w3.org/1999/XMLSchema";
    public static final String XML_SCHEMA_URI3 = "http://www.w3.org/2000/10/XMLSchema";
    public static final String[] SCHEMA_URIS = {
        XML_SCHEMA_URI,
        XML_SCHEMA_URI2,
        XML_SCHEMA_URI3
    };

    @Nullable
    public static String findLocalNameByQualifiedName(String name) {
        return name == null ? null : name.substring(name.indexOf(':') + 1);
    }
}
