package consulo.xml.util.xml.impl;

import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.xml.language.psi.XmlElementFactory;
import consulo.xml.language.psi.XmlDocument;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.dom.DomElement;
import consulo.xml.dom.DomFileElement;
import consulo.xml.dom.DomNameStrategy;
import consulo.xml.dom.EvaluatedXmlName;
import consulo.xml.util.xml.stubs.ElementStub;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author peter
 */
public class DomRootInvocationHandler extends DomInvocationHandler<AbstractDomChildDescriptionImpl, ElementStub> {
  private static final Logger LOG = Logger.getInstance("#DomRootInvocationHandler");
  private final DomFileElementImpl<?> myParent;

  public DomRootInvocationHandler(final Class aClass,
                                  final RootDomParentStrategy strategy,
                                  final DomFileElementImpl fileElement,
                                  final EvaluatedXmlName tagName,
                                  @Nullable ElementStub stub
  ) {
    super(aClass, strategy, tagName, new AbstractDomChildDescriptionImpl(aClass) {
      public List<? extends DomElement> getValues(final DomElement parent) {
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

  public DomFileElementImpl getParent() {
    return myParent;
  }

  public DomElement createPathStableCopy() {
    final DomFileElement stableCopy = myParent.createStableCopy();
    return getManager().createStableValue(() -> stableCopy.isValid() ? stableCopy.getRootElement() : null);
  }

  protected XmlTag setEmptyXmlTag() {
    final XmlTag[] result = new XmlTag[]{null};
    getManager().runChange(new Runnable() {
      public void run() {
        try {
          final String namespace = getXmlElementNamespace();
          final String nsDecl = StringUtil.isEmpty(namespace) ? "" : " xmlns=\"" + namespace + "\"";
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

  public final DomNameStrategy getNameStrategy() {
    final Class<?> rawType = getRawType();
    final DomNameStrategy strategy = DomImplUtil.getDomNameStrategy(rawType, isAttribute());
    if (strategy != null) {
      return strategy;
    }
    return DomNameStrategy.HYPHEN_STRATEGY;
  }


}
