package consulo.xml.util.xml.impl;

import consulo.xml.util.xml.ConvertContext;
import consulo.xml.util.xml.DomElement;

public class ConvertContextFactory {
   public static ConvertContext createConvertContext(final DomElement element) {
      return new ConvertContextImpl(DomManagerImpl.getDomInvocationHandler(element)) {
        public DomElement getInvocationElement() {
           return element;
        }
      };
   }

   public static ConvertContext createConvertContext(final DomInvocationHandler element) {
     return new ConvertContextImpl(element);
   }
}
