/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.xml.psi.impl.source.resolve.reference.impl.providers;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.intention.QuickFixActionRegistrar;
import consulo.language.editor.intention.UnresolvedReferenceQuickFixProvider;
import consulo.localize.LocalizeValue;
import consulo.xml.localize.XmlLocalize;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

/**
 * @author yole
 */
@ExtensionImpl
public class SchemaReferenceQuickFixProvider extends UnresolvedReferenceQuickFixProvider<TypeOrElementOrAttributeReference> {
    @Override
    public void registerFixes(@Nonnull TypeOrElementOrAttributeReference ref, @Nonnull QuickFixActionRegistrar registrar) {
        if (ref.getType() == TypeOrElementOrAttributeReference.ReferenceType.TypeReference) {
            registrar.register(new CreateXmlElementIntentionAction(
                XmlLocalize::xmlSchemaCreateComplexTypeIntentionName,
                SchemaReferencesProvider.COMPLEX_TYPE_TAG_NAME,
                ref
            ));
            registrar.register(new CreateXmlElementIntentionAction(
                XmlLocalize::xmlSchemaCreateSimpleTypeIntentionName,
                SchemaReferencesProvider.SIMPLE_TYPE_TAG_NAME,
                ref
            ));
        }
        else if (ref.getType() != null) {
            Function<String, LocalizeValue> textPattern = null;
            String declarationTagName = null;

            if (ref.getType() == TypeOrElementOrAttributeReference.ReferenceType.ElementReference) {
                declarationTagName = SchemaReferencesProvider.ELEMENT_TAG_NAME;
                textPattern = XmlLocalize::xmlSchemaCreateElementIntentionName;
            }
            else if (ref.getType() == TypeOrElementOrAttributeReference.ReferenceType.AttributeReference) {
                declarationTagName = SchemaReferencesProvider.ATTRIBUTE_TAG_NAME;
                textPattern = XmlLocalize::xmlSchemaCreateAttributeIntentionName;
            }
            else if (ref.getType() == TypeOrElementOrAttributeReference.ReferenceType.AttributeGroupReference) {
                declarationTagName = SchemaReferencesProvider.ATTRIBUTE_GROUP_TAG_NAME;
                textPattern = XmlLocalize::xmlSchemaCreateAttributeGroupIntentionName;
            }
            else if (ref.getType() == TypeOrElementOrAttributeReference.ReferenceType.GroupReference) {
                declarationTagName = SchemaReferencesProvider.GROUP_TAG_NAME;
                textPattern = XmlLocalize::xmlSchemaCreateGroupIntentionName;
            }

            assert textPattern != null;
            registrar.register(new CreateXmlElementIntentionAction(textPattern, declarationTagName, ref));
        }
    }

    @Nonnull
    @Override
    public Class<TypeOrElementOrAttributeReference> getReferenceClass() {
        return TypeOrElementOrAttributeReference.class;
    }
}
