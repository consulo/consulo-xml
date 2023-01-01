package consulo.xml.util.xml;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import com.intellij.xml.util.XmlUtil;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.language.editor.CodeInsightBundle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author peter
 */
public class EnumConverter<T extends Enum> extends ResolvingConverter<T>
{
	private static final Map<Class, EnumConverter> ourCache = ConcurrentFactoryMap.createMap(EnumConverter::new);

	private final Class<T> myType;

	private EnumConverter(final Class<T> aClass)
	{
		myType = aClass;
	}

	public static <T extends Enum> EnumConverter<T> createEnumConverter(Class<T> aClass)
	{
		return ourCache.get(aClass);
	}

	private String getStringValue(final T anEnum)
	{
		return NamedEnumUtil.getEnumValueByElement(anEnum);
	}

	public final T fromString(final String s, final ConvertContext context)
	{
		return s == null ? null : (T) NamedEnumUtil.getEnumElementByValue((Class) myType, s);
	}

	public final String toString(final T t, final ConvertContext context)
	{
		return t == null ? null : getStringValue(t);
	}

	public String getErrorMessage(@Nullable final String s, final ConvertContext context)
	{
		return CodeInsightBundle.message("error.unknown.enum.value.message", s);
	}

	@Nonnull
	public Collection<? extends T> getVariants(final ConvertContext context)
	{
		final XmlElement element = context.getXmlElement();
		if(element instanceof XmlTag)
		{
			final XmlTag simpleContent = XmlUtil.getSchemaSimpleContent((XmlTag) element);
			if(simpleContent != null && XmlUtil.collectEnumerationValues(simpleContent, new HashSet<String>()))
			{
				return Collections.emptyList();
			}
		}
		return Arrays.asList(myType.getEnumConstants());
	}
}
