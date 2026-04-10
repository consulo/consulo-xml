package consulo.xml.dom.convert;

import consulo.application.util.ConcurrentFactoryMap;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import consulo.xml.descriptor.xsd.XsdSchemeUtil;
import consulo.xml.dom.ConvertContext;
import consulo.xml.dom.NamedEnumUtil;
import consulo.xml.dom.ResolvingConverter;
import consulo.xml.dom.localize.DomLocalize;
import consulo.xml.language.psi.XmlElement;
import consulo.xml.language.psi.XmlTag;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author peter
 */
public class EnumConverter<T extends Enum> extends ResolvingConverter<T> {
    private static final Map<Class, EnumConverter> ourCache = ConcurrentFactoryMap.createMap(EnumConverter::new);

    private final Class<T> myType;

    private EnumConverter(final Class<T> aClass) {
        myType = aClass;
    }

    public static <T extends Enum> EnumConverter<T> createEnumConverter(Class<T> aClass) {
        return ourCache.get(aClass);
    }

    private String getStringValue(final T anEnum) {
        return NamedEnumUtil.getEnumValueByElement(anEnum);
    }

    @Override
    public final T fromString(final String s, final ConvertContext context) {
        return s == null ? null : (T) NamedEnumUtil.getEnumElementByValue((Class) myType, s);
    }

    @Override
    public final String toString(final T t, final ConvertContext context) {
        return t == null ? null : getStringValue(t);
    }

    @Override
    public LocalizeValue buildUnresolvedMessage(@Nullable final String s, final ConvertContext context) {
        return DomLocalize.errorUnknownEnumValueMessage(StringUtil.notNullize(s));
    }

    @Override
    public Collection<? extends T> getVariants(final ConvertContext context) {
        final XmlElement element = context.getXmlElement();
        if (element instanceof XmlTag) {
            final XmlTag simpleContent = XsdSchemeUtil.getSchemaSimpleContent((XmlTag) element);
            if (simpleContent != null && XsdSchemeUtil.collectEnumerationValues(simpleContent, new HashSet<String>())) {
                return Collections.emptyList();
            }
        }
        return Arrays.asList(myType.getEnumConstants());
    }
}
