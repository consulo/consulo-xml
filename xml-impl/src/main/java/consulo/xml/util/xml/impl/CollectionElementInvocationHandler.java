package consulo.xml.util.xml.impl;

import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.EvaluatedXmlName;
import consulo.xml.util.xml.events.DomEvent;
import consulo.xml.util.xml.stubs.ElementStub;
import consulo.xml.util.xml.stubs.StubParentStrategy;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class CollectionElementInvocationHandler extends DomInvocationHandler<AbstractDomChildDescriptionImpl, ElementStub>{

  public CollectionElementInvocationHandler(final Type type, @Nonnull final XmlTag tag,
                                            final AbstractCollectionChildDescription description,
                                            final DomInvocationHandler parent,
                                            @Nullable ElementStub stub) {
    super(type, new PhysicalDomParentStrategy(tag, parent.getManager()), description.createEvaluatedXmlName(parent, tag),
          (AbstractDomChildDescriptionImpl)description, parent.getManager(), true, stub);
  }

  public CollectionElementInvocationHandler(@Nonnull EvaluatedXmlName tagName,
                                            AbstractDomChildDescriptionImpl childDescription,
                                            DomManagerImpl manager,
                                            ElementStub stub) {
    super(childDescription.getType(), new StubParentStrategy(stub), tagName, childDescription, manager, true, stub);

  }

  protected Type narrowType(@Nonnull final Type nominalType) {
    return getStub() == null ? getManager().getTypeChooserManager().getTypeChooser(nominalType).chooseType(getXmlTag()) : nominalType;
  }

  protected final XmlTag setEmptyXmlTag() {
    throw new UnsupportedOperationException("CollectionElementInvocationHandler.setXmlTag() shouldn't be called;" +
                                            "\nparent=" + getParent() + ";\n" +
                                            "xmlElementName=" + getXmlElementName());
  }

  @Override
  protected String checkValidity() {
    final String s = super.checkValidity();
    if (s != null) {
      return s;
    }

    if (getXmlTag() == null) {
      return "no XmlTag for collection element: " + getDomElementType();
    }

    return null;
  }

  public final void undefineInternal() {
    final DomElement parent = getParent();
    final XmlTag tag = getXmlTag();
    if (tag == null) return;

    getManager().cacheHandler(getCacheKey(), tag, null);
    deleteTag(tag);
    getManager().fireEvent(new DomEvent(parent, false));
  }

  public DomElement createPathStableCopy() {
    final AbstractDomChildDescriptionImpl description = getChildDescription();
    final DomElement parent = getParent();
    assert parent != null;
    final DomElement parentCopy = parent.createStableCopy();
    final int index = description.getValues(parent).indexOf(getProxy());
    return getManager().createStableValue(new Supplier<DomElement>() {
      @Nullable
      public DomElement get() {
        if (parentCopy.isValid()) {
          final List<? extends DomElement> list = description.getValues(parentCopy);
          if (list.size() > index) {
            return list.get(index);
          }
        }
        return null;
      }
    });
  }

  @Override
  public int hashCode() {
    ElementStub stub = getStub();
    if (stub != null) {
      return stub.getName().hashCode() + stub.getStubId();
    }
    final XmlElement element = getXmlElement();
    return element == null ? super.hashCode() : element.hashCode();
  }
}
