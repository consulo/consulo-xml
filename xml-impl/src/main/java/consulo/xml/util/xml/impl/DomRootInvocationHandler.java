package consulo.xml.util.xml.impl;

import consulo.ide.impl.idea.openapi.util.NullableFactory;
import consulo.logging.Logger;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.language.util.IncorrectOperationException;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomFileElement;
import consulo.xml.util.xml.DomNameStrategy;
import consulo.xml.util.xml.EvaluatedXmlName;
import consulo.xml.util.xml.stubs.ElementStub;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author peter
 */
public class DomRootInvocationHandler extends DomInvocationHandler<AbstractDomChildDescriptionImpl, ElementStub> {
  private static final Logger LOG = Logger.getInstance("#DomRootInvocationHandler");
  private final DomFileElementImpl<?> myParent;

  public DomRootInvocationHandler(final Class aClass,
                                  final RootDomParentStrategy strategy,
                                  @Nonnull final DomFileElementImpl fileElement,
                                  @Nonnull final EvaluatedXmlName tagName,
                                  @Nullable ElementStub stub
  ) {
    super(aClass, strategy, tagName, new AbstractDomChildDescriptionImpl(aClass) {
      @Nonnull
      public List<? extends DomElement> getValues(@Nonnull final DomElement parent) {
        throw new UnsupportedOperationException();
      }

      public int compareTo(final AbstractDomChildDescriptionImpl o) {
        throw new UnsupportedOperationException();
      }
    }, fileElement.getManager(), true, stub);
    myParent = fileElement;
  }

  public void undefineInternal() {
    try {
      final XmlTag tag = getXmlTag();
      if (tag != null) {
        deleteTag(tag);
        detach();
        fireUndefinedEvent();
      }
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  public boolean equals(final Object obj) {
    if (!(obj instanceof DomRootInvocationHandler)) return false;

    final DomRootInvocationHandler handler = (DomRootInvocationHandler)obj;
    return myParent.equals(handler.myParent);
  }

  public int hashCode() {
    return myParent.hashCode();
  }

  @Nonnull
  public String getXmlElementNamespace() {
    return getXmlName().getNamespace(getFile(), getFile());
  }

  @Override
  protected String checkValidity() {
    final XmlTag tag = (XmlTag)getXmlElement();
    if (tag != null && !tag.isValid()) {
      return "invalid root tag";
    }

    final String s = myParent.checkValidity();
    if (s != null) {
      return "root: " + s;
    }

    return null;
  }

  @Nonnull
  public DomFileElementImpl getParent() {
    return myParent;
  }

  public DomElement createPathStableCopy() {
    final DomFileElement stableCopy = myParent.createStableCopy();
    return getManager().createStableValue(new NullableFactory<DomElement>() {
      public DomElement create() {
        return stableCopy.isValid() ? stableCopy.getRootElement() : null;
      }
    });
  }

  protected XmlTag setEmptyXmlTag() {
    final XmlTag[] result = new XmlTag[]{null};
    getManager().runChange(new Runnable() {
      public void run() {
        try {
          final String namespace = getXmlElementNamespace();
          @NonNls final String nsDecl = StringUtil.isEmpty(namespace) ? "" : " xmlns=\"" + namespace + "\"";
          final XmlFile xmlFile = getFile();
          final XmlTag tag = XmlElementFactory.getInstance(xmlFile.getProject()).createTagFromText("<" + getXmlElementName() + nsDecl + "/>");
          result[0] = ((XmlDocument)xmlFile.getDocument().replace(((XmlFile)tag.getContainingFile()).getDocument())).getRootTag();
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    });
    return result[0];
  }

  @Nonnull
  public final DomNameStrategy getNameStrategy() {
    final Class<?> rawType = getRawType();
    final DomNameStrategy strategy = DomImplUtil.getDomNameStrategy(rawType, isAttribute());
    if (strategy != null) {
      return strategy;
    }
    return DomNameStrategy.HYPHEN_STRATEGY;
  }


}
