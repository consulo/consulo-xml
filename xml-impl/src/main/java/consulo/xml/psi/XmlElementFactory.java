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
package consulo.xml.psi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import org.jetbrains.annotations.NonNls;
import consulo.ide.ServiceManager;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.psi.xml.XmlText;
import consulo.language.util.IncorrectOperationException;

/**
 * @author Dmitry Avdeev
 */
public abstract class XmlElementFactory
{

	public static XmlElementFactory getInstance(Project project)
	{
		return ServiceManager.getService(project, XmlElementFactory.class);
	}

	/**
	 * Creates an XML text element from the specified string, escaping the special
	 * characters in the string as necessary.
	 *
	 * @param s the text of the element to create.
	 * @return the created element.
	 * @throws IncorrectOperationException if the creation failed for some reason.
	 */
	@Nonnull
	public abstract XmlText createDisplayText(@Nonnull @NonNls String s) throws IncorrectOperationException;

	/**
	 * Creates an XHTML tag with the specified text.
	 *
	 * @param s the text of an XHTML tag (which can contain attributes and subtags).
	 * @return the created tag instance.
	 * @throws IncorrectOperationException if the text does not specify a valid XML fragment.
	 */
	@Nonnull
	public abstract XmlTag createXHTMLTagFromText(@Nonnull @NonNls String s) throws IncorrectOperationException;

	/**
	 * Creates an HTML tag with the specified text.
	 *
	 * @param s the text of an HTML tag (which can contain attributes and subtags).
	 * @return the created tag instance.
	 * @throws IncorrectOperationException if the text does not specify a valid XML fragment.
	 */
	@Nonnull
	public abstract XmlTag createHTMLTagFromText(@Nonnull @NonNls String s) throws IncorrectOperationException;

	/**
	 * Creates an XML tag with the specified text.
	 *
	 * @param text the text of an XML tag (which can contain attributes and subtags).
	 * @return the created tag instance.
	 * @throws IncorrectOperationException if the text does not specify a valid XML fragment.
	 * @see #createTagFromText(CharSequence text, Language language)
	 */
	@Nonnull
	public abstract XmlTag createTagFromText(@Nonnull @NonNls CharSequence text) throws IncorrectOperationException;

	/**
	 * Creates XML like tag with the specified text and language.
	 *
	 * @param text     the text of an XML tag (which can contain attributes and subtags).
	 * @param language the language for tag to be created.
	 * @return the created tag instance.
	 * @throws IncorrectOperationException if the text does not specify a valid XML fragment.
	 * @see #createTagFromText(CharSequence)
	 */
	@Nonnull
	public abstract XmlTag createTagFromText(@Nonnull @NonNls CharSequence text, @Nonnull Language language) throws IncorrectOperationException;

	/**
	 * Creates an XML attribute with the specified name and value.
	 *
	 * @param name  the name of the attribute to create.
	 * @param value the value of the attribute to create.
	 * @return the created attribute instance.
	 * @throws IncorrectOperationException if either {@code name} or {@code value} are not valid.
	 */
	@Nonnull
	public abstract XmlAttribute createXmlAttribute(@Nonnull @NonNls String name, @Nonnull String value) throws consulo.language.util.IncorrectOperationException;

	/**
	 * Creates an attribute with the specified name and value  with given context.
	 *
	 * @param name    the name of the attribute to create.
	 * @param value   the value of the attribute to create.
	 * @param context element which can be used to determine created attribute file type.
	 * @return the created attribute instance.
	 * @throws IncorrectOperationException if either {@code name} or {@code value} are not valid.
	 */
	@Nonnull
	public abstract XmlAttribute createAttribute(@Nonnull @NonNls String name, @Nonnull String value, @Nullable PsiElement context) throws IncorrectOperationException;
}
