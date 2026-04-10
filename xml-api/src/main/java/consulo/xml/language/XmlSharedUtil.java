package consulo.xml.language;

import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.xml.language.psi.util.XmlTagUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-04-10
 */
public class XmlSharedUtil {
    private static final Logger LOG = Logger.getInstance(XmlSharedUtil.class);

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

    public static CharSequence getLocalName(final CharSequence tagName) {
        int pos = StringUtil.indexOf(tagName, ':');
        if (pos == -1) {
            return tagName;
        }
        return tagName.subSequence(pos + 1, tagName.length());
    }

    public static char getCharFromEntityRef(String text) {
        try {
            if (text.charAt(1) != '#') {
                text = text.substring(1, text.length() - 1);
                return XmlTagUtil.getCharacterByEntityName(text);
            }
            text = text.substring(2, text.length() - 1);
        }
        catch (StringIndexOutOfBoundsException e) {
            LOG.error("Cannot parse ref: '" + text + "'", e);
        }
        try {
            int code;
            if (StringUtil.startsWithChar(text, 'x')) {
                text = text.substring(1);
                code = Integer.parseInt(text, 16);
            }
            else {
                code = Integer.parseInt(text);
            }
            return (char) code;
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }
}
