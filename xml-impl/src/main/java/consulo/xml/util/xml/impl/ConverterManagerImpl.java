package consulo.xml.util.xml.impl;

import consulo.application.Application;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.language.psi.path.PathReference;
import consulo.xml.dom.ConverterImplementationProvider;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.converters.PathReferenceConverter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author peter
 */
@Singleton
public class ConverterManagerImpl implements ConverterManager {
  private final ImplementationClassCache<ConverterImplementationProvider> myImplementationClassCache = new ImplementationClassCache<>(ConverterImplementationProvider.class);

  private final ConcurrentMap<Class, Object> myConverterInstances = ConcurrentFactoryMap.createMap(key -> {
    ConverterImplementationProvider implementation = myImplementationClassCache.get(key);
    return Application.get().getUnbindedInstance(implementation == null ? key : implementation.getImplementationClass());
  });

  private final Map<Class, Converter> mySimpleConverters = new HashMap<>();

  @Inject
  public ConverterManagerImpl() {
    mySimpleConverters.put(int.class, Converter.INTEGER_CONVERTER);
    mySimpleConverters.put(Integer.class, Converter.INTEGER_CONVERTER);
    mySimpleConverters.put(boolean.class, ResolvingConverter.BOOLEAN_CONVERTER);
    mySimpleConverters.put(Boolean.class, ResolvingConverter.BOOLEAN_CONVERTER);
    mySimpleConverters.put(String.class, Converter.EMPTY_CONVERTER);
    mySimpleConverters.put(Object.class, Converter.EMPTY_CONVERTER);
    mySimpleConverters.put(PathReference.class, PathReferenceConverter.INSTANCE);
  }

  public void addConverter(Class clazz, Converter converter) {
    mySimpleConverters.put(clazz, converter);
  }

  @Nonnull
  public final Converter getConverterInstance(final Class<? extends Converter> converterClass) {
    Converter converter = getInstance(converterClass);
    assert converter != null : "Converter not found for " + converterClass;
    return converter;
  }

  <T> T getInstance(Class<T> clazz) {
    return (T) myConverterInstances.get(clazz);
  }

  @Nullable
  public final Converter getConverterByClass(final Class<?> convertingClass) {
    final Converter converter = mySimpleConverters.get(convertingClass);
    if (converter != null) {
      return converter;
    }

    if (Enum.class.isAssignableFrom(convertingClass)) {
      return EnumConverter.createEnumConverter((Class<? extends Enum>) convertingClass);
    }
    if (DomElement.class.isAssignableFrom(convertingClass)) {
      return DomResolveConverter.createConverter((Class<? extends DomElement>) convertingClass);
    }
    return null;
  }

  public <T extends Converter> void registerConverterImplementation(Class<T> converterInterface, T converterImpl) {
    myConverterInstances.put(converterInterface, converterImpl);
  }
}
