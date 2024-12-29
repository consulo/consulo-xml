/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.xml.javaee;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.xml.impl.internal.StandardExternalResourceData;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@State(name = "ProjectResources", storages = @Storage("misc.xml"))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ProjectExternalResourceManagerImpl extends ExternalResourceManagerExImpl
{
	private StandardExternalResourceData myData = new StandardExternalResourceData(Map.of(), Set.of());

	@Nonnull
	@Override
	protected StandardExternalResourceData getData()
	{
		return myData;
	}
}
