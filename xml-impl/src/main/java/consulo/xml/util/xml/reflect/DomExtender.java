/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.xml.util.xml.reflect;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.xml.util.xml.DomElement;

import jakarta.annotation.Nonnull;

/**
 * Register DOM extenders via dom.extender extension point. Specify 2 attributes:
 * domClass - the DOM element class for which this extender will be called. Should be equal to T.
 * extenderClass - this class qualified name.
 *
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class DomExtender<T extends DomElement>
{
	@Nonnull
	public abstract Class<T> getElementClass();

	/**
	 * @param t         DOM element where new children may be added to
	 * @param registrar a place to register your own DOM children descriptions
	 */
	public abstract void registerExtensions(@Nonnull T t, @Nonnull final DomExtensionsRegistrar registrar);
}
