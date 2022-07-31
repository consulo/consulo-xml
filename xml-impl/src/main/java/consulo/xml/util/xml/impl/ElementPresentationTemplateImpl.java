/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.xml.util.xml.impl;

import consulo.application.presentation.TypePresentationService;
import consulo.ui.image.Image;
import consulo.util.lang.ref.Ref;
import consulo.xml.util.xml.*;

/**
 * @author Dmitry Avdeev
 */
public class ElementPresentationTemplateImpl implements ElementPresentationTemplate
{
	public static final ElementPresentationTemplate INSTANCE = new ElementPresentationTemplateImpl();

	@Override
	public ElementPresentation createPresentation(final DomElement element)
	{
		return new ElementPresentation()
		{
			@Override
			public String getElementName()
			{
				return TypePresentationService.getInstance().getPresentableName(element);
			}

			@Override
			public String getTypeName()
			{
				return TypePresentationService.getInstance().getTypeName(element);
			}

			@Override
			public Image getIcon()
			{
				return TypePresentationService.getInstance().getIcon(element);
			}

			@Override
			public String getDocumentation()
			{
				final Ref<String> result = new Ref<String>();
				element.acceptChildren(new DomElementVisitor()
				{
					@Override
					public void visitDomElement(DomElement element)
					{
						if(element instanceof GenericValue && element.getChildDescription().getAnnotation(Documentation.class) != null)
						{
							result.set(((GenericValue) element).getStringValue());
						}
					}
				});
				return result.isNull() ? super.getDocumentation() : result.get();
			}
		};
	}
}
