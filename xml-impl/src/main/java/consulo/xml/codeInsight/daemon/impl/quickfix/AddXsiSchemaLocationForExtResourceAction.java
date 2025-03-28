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
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.xml.codeInsight.daemon.impl.analysis.CreateNSDeclarationIntentionFix;
import consulo.xml.impl.localize.XmlErrorLocalize;
import consulo.xml.psi.XmlElementFactory;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

/**
 * @author maxim.mossienko
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "xml.add.xsi.schema.location.for.external.resource", fileExtensions = "xml", categories = "XML")
public class AddXsiSchemaLocationForExtResourceAction extends BaseExtResourceAction {
    @NonNls
    private static final String XMLNS_XSI_ATTR_NAME = "xmlns:xsi";
    @NonNls
    private static final String XSI_SCHEMA_LOCATION_ATTR_NAME = "xsi:schemaLocation";
    public static final String KEY = "add.xsi.schema.location.for.external.resource";

    protected String getQuickFixKeyId() {
        return KEY;
    }

    protected void doInvoke(
        @Nonnull final PsiFile file,
        final int offset,
        @Nonnull final String uri,
        final Editor editor
    ) throws IncorrectOperationException {
        final XmlTag tag = PsiTreeUtil.getParentOfType(file.findElementAt(offset), XmlTag.class);
        if (tag == null) {
            return;
        }
        final List<String> schemaLocations = new ArrayList<String>();

        CreateNSDeclarationIntentionFix.processExternalUris(
            new CreateNSDeclarationIntentionFix.TagMetaHandler(tag.getLocalName()),
            file,
            new CreateNSDeclarationIntentionFix.ExternalUriProcessor() {
                public void process(@Nonnull final String currentUri, final String url) {
                    if (currentUri.equals(uri) && url != null) {
                        schemaLocations.add(url);
                    }
                }

            },
            true
        );

        CreateNSDeclarationIntentionFix.runActionOverSeveralAttributeValuesAfterLettingUserSelectTheNeededOne(
            ArrayUtil.toStringArray(schemaLocations), file.getProject(), new CreateNSDeclarationIntentionFix.StringToAttributeProcessor() {
                public void doSomethingWithGivenStringToProduceXmlAttributeNowPlease(@Nonnull final String attrName) throws IncorrectOperationException {
                    doIt(file, editor, uri, tag, attrName);
                }
            }, XmlErrorLocalize.selectNamespaceLocationTitle().get(), this, editor);
    }

    private static void doIt(
        final PsiFile file,
        final Editor editor,
        final String uri,
        final XmlTag tag,
        final String s
    ) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        final XmlElementFactory elementFactory = XmlElementFactory.getInstance(file.getProject());

        if (tag.getAttributeValue(XMLNS_XSI_ATTR_NAME) == null) {
            tag.add(elementFactory.createXmlAttribute(XMLNS_XSI_ATTR_NAME, XmlUtil.XML_SCHEMA_INSTANCE_URI));
        }

        final XmlAttribute locationAttribute = tag.getAttribute(XSI_SCHEMA_LOCATION_ATTR_NAME);
        final String toInsert = uri + " " + s;
        int offset = s.length();

        if (locationAttribute == null) {
            tag.add(elementFactory.createXmlAttribute(XSI_SCHEMA_LOCATION_ATTR_NAME, toInsert));
        }
        else {
            final String newValue = locationAttribute.getValue() + "\n" + toInsert;
            locationAttribute.setValue(newValue);
        }

        CodeStyleManager.getInstance(file.getProject()).reformat(tag);

        @SuppressWarnings("ConstantConditions") final TextRange range =
            tag.getAttribute(XSI_SCHEMA_LOCATION_ATTR_NAME).getValueElement().getTextRange();
        final TextRange textRange = new TextRange(range.getEndOffset() - offset - 1, range.getEndOffset() - 1);
        editor.getCaretModel().moveToOffset(textRange.getStartOffset());
    }

    @Override
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
        if (attribute != null && attribute.isNamespaceDeclaration()) {
            return true;
        }
        return false;
    }
}