/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.xml.codeInsight.intentions;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ColorPickerBuilder;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.StandardColors;
import consulo.ui.util.ColorValueUtil;
import consulo.util.lang.StringUtil;
import consulo.xml.language.psi.XmlElementFactory;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
@IntentionMetaData(ignoreId = "xml.choose.color", fileExtensions = "xml", categories = "XML")
public class XmlChooseColorIntentionAction extends PsiElementBaseIntentionAction {
    public XmlChooseColorIntentionAction() {
        setText(CodeInsightLocalize.intentionColorChooserDialog());
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof XmlAttributeValue attrValue) {
            try {
                return ColorValueUtil.fromHex(attrValue.getValue()) != null;
            }
            catch (Exception ignored) {
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public void invoke(Project project, Editor editor, PsiElement element) throws IncorrectOperationException {
        chooseColor(editor.getUIComponent(), element, getText());
    }

    @RequiredUIAccess
    public static void chooseColor(Component editorComponent, PsiElement element, LocalizeValue caption) {
        XmlAttributeValue literal = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class, false);
        if (literal == null) {
            return;
        }
        String text = StringUtil.unquoteString(literal.getValue());

        ColorValue oldColor;
        try {
            oldColor = ColorValueUtil.fromHex(text);
        }
        catch (NumberFormatException e) {
            oldColor = StandardColors.GRAY;
        }

        ColorValue temp = oldColor;
        ColorPickerBuilder.create()
            .withTitle(caption)
            .withColor(temp)
            .withAlpha()
            .showAsync(editorComponent)
            .whenComplete((colorValue, throwable) -> {
                if (colorValue == null) {
                    return;
                }

                if (!Objects.equals(colorValue, temp)) {
                    if (!FileModificationService.getInstance().preparePsiElementForWrite(element)) {
                        return;
                    }

                    String newText = "#" + ColorValueUtil.toHex(colorValue);
                    PsiManager manager = literal.getManager();
                    XmlAttribute newAttribute =
                        XmlElementFactory.getInstance(manager.getProject()).createXmlAttribute("name", newText);

                    new WriteCommandAction(element.getProject(), caption.get()) {
                        @Override
                        protected void run(Result result) throws Throwable {
                            XmlAttributeValue valueElement = newAttribute.getValueElement();
                            assert valueElement != null;
                            literal.replace(valueElement);
                        }
                    }.execute();
                }
            });
    }
}
