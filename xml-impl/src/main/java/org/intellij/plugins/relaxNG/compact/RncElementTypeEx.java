/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.intellij.plugins.relaxNG.compact;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementTypeAsPsiFactory;
import org.intellij.plugins.relaxNG.compact.psi.RncElement;

import jakarta.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author nik
 */
class RncElementTypeEx<C extends RncElement> extends RncElementType implements IElementTypeAsPsiFactory
{
	private final Function<ASTNode, ? extends C> myFactory;

	RncElementTypeEx(String name, Function<ASTNode, ? extends C> factory)
	{
		super(name);
		myFactory = factory;
	}

	@Override
	@Nonnull
	public final C createElement(@Nonnull ASTNode node)
	{
		return myFactory.apply(node);
	}
}
