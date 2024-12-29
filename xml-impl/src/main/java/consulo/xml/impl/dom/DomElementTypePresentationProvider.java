package consulo.xml.impl.dom;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.presentation.TypePresentationProvider;
import consulo.component.util.Iconable;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.ElementPresentationManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29-Jul-22
 */
@ExtensionImpl
public class DomElementTypePresentationProvider extends TypePresentationProvider<DomElement>
{
	@Nonnull
	@Override
	public Class<DomElement> getItemClass()
	{
		return DomElement.class;
	}

	@Nullable
	@Override
	public String getName(DomElement domElement)
	{
		return ElementPresentationManager.getElementName(domElement);
	}

	@Nullable
	@Override
	public String getTypeName(DomElement element)
	{
		return StringUtil.capitalizeWords(element.getNameStrategy().splitIntoWords(element.getXmlElementName()), true);
	}

	@Nullable
	@Override
	public Image getIcon(DomElement domElement)
	{
		if(domElement instanceof Iconable)
		{
			return ((Iconable) domElement).getIcon(0);
		}
		return null;
	}
}
