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
package consulo.xml.psi.xml;

import consulo.language.util.IncorrectOperationException;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.meta.PsiMetaOwner;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * @author Mike
 */
public interface XmlTag extends XmlElement, PsiNamedElement, PsiMetaOwner, XmlTagChild
{
	XmlTag[] EMPTY = new XmlTag[0];

	@Override
	@Nonnull
	@NonNls
	String getName();

	@Nonnull
	@NonNls
	String getNamespace();

	@Nonnull
	@NonNls
	String getLocalName();

	@Nullable
	XmlElementDescriptor getDescriptor();

	@Nonnull
	XmlAttribute[] getAttributes();

	@Nullable
	XmlAttribute getAttribute(@NonNls String name, @NonNls String namespace);

	/**
	 * Returns a tag attribute by qualified name.
	 *
	 * @param qname qualified attribute name, like "ns:name" or "name".
	 * @return null if the attribute not exist.
	 * @see #getAttribute(String, String)
	 */
	@Nullable
	XmlAttribute getAttribute(@NonNls String qname);

	@Nullable
	String getAttributeValue(@NonNls String name, @NonNls String namespace);

	/**
	 * Returns a tag attribute value by qualified name.
	 *
	 * @param qname qualified attribute name, like "ns:name" or "name".
	 * @return null if the attribute not exist.
	 * @see #getAttributeValue(String, String)
	 */
	@Nullable
	String getAttributeValue(@NonNls String qname);

	XmlAttribute setAttribute(@NonNls String name, @NonNls String namespace, @NonNls String value) throws IncorrectOperationException;

	XmlAttribute setAttribute(@NonNls String qname, @NonNls String value) throws IncorrectOperationException;

	/**
	 * Creates a new child tag
	 *
	 * @param localName             new tag's name
	 * @param namespace             new tag's namespace
	 * @param bodyText              pass null to create collapsed tag, empty string means creating expanded one
	 * @param enforceNamespacesDeep if you pass some xml tags to {@code bodyText} parameter, this flag sets namespace prefixes for them
	 * @return created tag. Use {@link #addSubTag(XmlTag, boolean)}} to add it to parent
	 */
	XmlTag createChildTag(@NonNls String localName, @NonNls String namespace, @Nullable @NonNls String bodyText, boolean enforceNamespacesDeep);

	XmlTag addSubTag(XmlTag subTag, boolean first);

	@Nonnull
	XmlTag[] getSubTags();

	@Nonnull
	XmlTag[] findSubTags(@NonNls String qname);

	/**
	 * @param localName non-qualified tag name.
	 * @param namespace if null, name treated as qualified name to find.
	 */
	@Nonnull
	XmlTag[] findSubTags(@NonNls String localName, @Nullable String namespace);

	@Nullable
	XmlTag findFirstSubTag(@NonNls String qname);

	@Nonnull
	@NonNls
	String getNamespacePrefix();

	@Nonnull
	@NonNls
	String getNamespaceByPrefix(@NonNls String prefix);

	@Nullable
	String getPrefixByNamespace(@NonNls String namespace);

	String[] knownNamespaces();

	boolean hasNamespaceDeclarations();

	/**
	 * @return map keys: prefixes values: namespaces
	 */
	@Nonnull
	Map<String, String> getLocalNamespaceDeclarations();

	@Nonnull
	XmlTagValue getValue();

	@Nullable
	XmlNSDescriptor getNSDescriptor(@NonNls String namespace, boolean strict);

	boolean isEmpty();

	void collapseIfEmpty();

	@Nullable
	@NonNls
	String getSubTagText(@NonNls String qname);

	default boolean isCaseSensitive()
	{
		return true;
	}
}
