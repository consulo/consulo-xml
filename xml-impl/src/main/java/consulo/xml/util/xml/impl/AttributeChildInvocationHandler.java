package consulo.xml.util.xml.impl;

import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.DomElementVisitor;
import consulo.xml.util.xml.EvaluatedXmlName;
import consulo.xml.util.xml.GenericAttributeValue;
import consulo.xml.util.xml.events.DomEvent;
import consulo.xml.util.xml.stubs.AttributeStub;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public class AttributeChildInvocationHandler extends DomInvocationHandler<AttributeChildDescriptionImpl, AttributeStub> {
  private static final Logger LOG = Logger.getInstance("#AttributeChildInvocationHandler");

  protected AttributeChildInvocationHandler(final EvaluatedXmlName attributeName,
                                            final AttributeChildDescriptionImpl description,
                                            final DomManagerImpl manager,
                                            final DomParentStrategy strategy,
                                            @Nullable AttributeStub stub) {
    super(description.getType(), strategy, attributeName, description, manager, false, stub);
  }

  public void acceptChildren(DomElementVisitor visitor) {
  }

  protected final XmlTag setEmptyXmlTag() {
    return ensureTagExists();
  }

  protected boolean isAttribute() {
    return true;
  }

  protected XmlElement recomputeXmlElement(@Nonnull final DomInvocationHandler parent) {
    if (!parent.isValid()) return null;

    final XmlTag tag = parent.getXmlTag();
    if (tag == null) return null;

    return tag.getAttribute(getXmlElementName(), getXmlApiCompatibleNamespace(parent));
  }

  @Nullable
  private String getXmlApiCompatibleNamespace(DomInvocationHandler parent) {
    final XmlTag tag = parent.getXmlTag();
    if (tag == null) {
      return null;
    }

    String ns = getXmlName().getNamespace(tag, parent.getFile());
    // TODO: this seems ugly
    return tag.getNamespace().equals(ns) ? null : ns;
  }

  public final XmlAttribute ensureXmlElementExists() {
    XmlAttribute attribute = (XmlAttribute)getXmlElement();
    if (attribute != null) return attribute;

    final DomManagerImpl manager = getManager();
    final boolean b = manager.setChanging(true);
    try {
      attribute = ensureTagExists().setAttribute(getXmlElementName(), getXmlApiCompatibleNamespace(getParentHandler()), "");
      setXmlElement(attribute);
      getManager().cacheHandler(DomManagerImpl.DOM_ATTRIBUTE_HANDLER_KEY, attribute, this);
      final DomElement element = getProxy();
      manager.fireEvent(new DomEvent(element, true));
      return attribute;
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
      return null;
    }
    finally {
      manager.setChanging(b);
    }
  }

  public <T extends DomElement> T createStableCopy() {
    final DomElement parentCopy = getParent().createStableCopy();
    return getManager().createStableValue(() -> parentCopy.isValid() ? (T) getChildDescription().getValues(parentCopy).get(0) : null);
  }

  public final void undefineInternal() {
    final XmlTag tag = getXmlTag();
    if (tag != null) {
      getManager().runChange(new Runnable() {
        public void run() {
          try {
            setXmlElement(null);
            tag.setAttribute(getXmlElementName(), getXmlApiCompatibleNamespace(getParentHandler()), null);
          }
          catch (consulo.language.util.IncorrectOperationException e) {
            LOG.error(e);
          }
        }
      });
      fireUndefinedEvent();
    }
  }

  @Nullable
  public final XmlTag getXmlTag() {
    final DomInvocationHandler handler = getParentHandler();
    return handler == null ? null : handler.getXmlTag();
  }

  public final XmlTag ensureTagExists() {
    final DomInvocationHandler parent = getParentHandler();
    assert parent != null : "write operations should be performed on the DOM having a parent, your DOM may be not very fresh";
    return parent.ensureTagExists();
  }

  @Nullable
  protected String getValue() {
    if (myStub != null) {
      return myStub.getValue();
    }
    final XmlAttribute attribute = (XmlAttribute)getXmlElement();
    if (attribute != null) {
      final XmlAttributeValue value = attribute.getValueElement();
      if (value != null && value.getTextLength() >= 2) {
        return attribute.getDisplayValue();
      }
    }
    return null;
  }

  public void copyFrom(final DomElement other) {
    setValue(((GenericAttributeValue) other).getStringValue());
  }

  protected void setValue(@Nullable final String value) {
    final XmlTag tag = ensureTagExists();
    final String attributeName = getXmlElementName();
    final String namespace = getXmlApiCompatibleNamespace(getParentHandler());
    final String oldValue = StringUtil.unescapeXml(tag.getAttributeValue(attributeName, namespace));
    final String newValue = XmlStringUtil.escapeString(value);
    if (Comparing.equal(oldValue, newValue, true)) return;

    getManager().runChange(new Runnable() {
      public void run() {
        try {
          XmlAttribute attribute = tag.setAttribute(attributeName, namespace, newValue);
          setXmlElement(attribute);
          getManager().cacheHandler(DomManagerImpl.DOM_ATTRIBUTE_HANDLER_KEY, attribute, AttributeChildInvocationHandler.this);
        }
        catch (consulo.language.util.IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    });
    final DomElement proxy = getProxy();
    final DomElement element = proxy;
    getManager().fireEvent(oldValue != null ? new DomEvent(proxy, false) : new DomEvent(element, true));
  }

}
