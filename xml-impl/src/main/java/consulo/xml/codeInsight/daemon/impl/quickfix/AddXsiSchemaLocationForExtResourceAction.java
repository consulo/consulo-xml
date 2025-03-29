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
package consulo.xml.codeInsight.daemon.impl.quickfix;

import com.intellij.xml.util.XmlUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.xml.codeInsight.daemon.impl.analysis.CreateNSDeclarationIntentionFix;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.impl.localize.XmlLocalize;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author maxim.mossienko
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "xml.add.xsi.schema.location.for.external.resource", fileExtensions = "xml", categories = "XML")
public class AddXsiSchemaLocationForExtResourceAction extends BaseExtResourceAction {
    private static final String XMLNS_XSI_ATTR_NAME = "xmlns:xsi";
    private static final String XSI_SCHEMA_LOCATION_ATTR_NAME = "xsi:schemaLocation";

    @Override
    protected @Nonnull LocalizeValue getQuickFixName() {
        return XmlLocalize.addXsiSchemaLocationForExternalResource();
    }

    @Override
    @RequiredUIAccess
    protected void doInvoke(@Nonnull PsiFile file, int offset, @Nonnull String uri, Editor editor) throws IncorrectOperationException {
        XmlTag tag = PsiTreeUtil.getParentOfType(file.findElementAt(offset), XmlTag.class);
        if (tag == null) {
            return;
        }
        List<String> schemaLocations = new ArrayList<>();

        CreateNSDeclarationIntentionFix.processExternalUris(
            new CreateNSDeclarationIntentionFix.TagMetaHandler(tag.getLocalName()),
            file,
            (currentUri, url) -> {
                if (currentUri.equals(uri) && url != null) {
                    schemaLocations.add(url);
                }
            },
            true
        );

        CreateNSDeclarationIntentionFix.runActionOverSeveralAttributeValuesAfterLettingUserSelectTheNeededOne(
            ArrayUtil.toStringArray(schemaLocations),
            file.getProject(),
            attrName -> doIt(file, editor, uri, tag, attrName),
            XmlErrorLocalize.selectNamespaceLocationTitle().get(),
            this,
            editor
        );
    }

    @RequiredUIAccess
    private static void doIt(PsiFile file, Editor editor, String uri, XmlTag tag, String s) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        XmlElementFactory elementFactory = XmlElementFactory.getInstance(file.getProject());

        if (tag.getAttributeValue(XMLNS_XSI_ATTR_NAME) == null) {
            tag.add(elementFactory.createXmlAttribute(XMLNS_XSI_ATTR_NAME, XmlUtil.XML_SCHEMA_INSTANCE_URI));
        }

        XmlAttribute locationAttribute = tag.getAttribute(XSI_SCHEMA_LOCATION_ATTR_NAME);
        String toInsert = uri + " " + s;
        int offset = s.length();

        if (locationAttribute == null) {
            tag.add(elementFactory.createXmlAttribute(XSI_SCHEMA_LOCATION_ATTR_NAME, toInsert));
        }
        else {
            String newValue = locationAttribute.getValue() + "\n" + toInsert;
            locationAttribute.setValue(newValue);
        }

        CodeStyleManager.getInstance(file.getProject()).reformat(tag);

        @SuppressWarnings("ConstantConditions") TextRange range =
            tag.getAttribute(XSI_SCHEMA_LOCATION_ATTR_NAME).getValueElement().getTextRange();
        TextRange textRange = new TextRange(range.getEndOffset() - offset - 1, range.getEndOffset() - 1);
        editor.getCaretModel().moveToOffset(textRange.getStartOffset());
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof XmlFile)) {
            return false;
        }

        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        XmlAttributeValue value = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
        if (value == null) {
            return false;
        }
        XmlAttribute attribute = PsiTreeUtil.getParentOfType(value, XmlAttribute.class);
        return attribute != null && attribute.isNamespaceDeclaration();
    }
}