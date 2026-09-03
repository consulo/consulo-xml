/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.xml.Html5SchemaProvider;
import com.intellij.xml.util.XmlUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.collection.Lists;

import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class HtmlLanguageLevelForm {
    enum HtmlOption {
        HTML4,
        HTML5,
        OTHER
    }

    private Layout myContentLayout;

    private RadioButton myHtml4RadioButton;
    private RadioButton myHtml5RadioButton;
    private RadioButton myOtherRadioButton;
    private TextBoxWithHistory myDoctypeTextBox;

    private final List<MyListener> myListeners = Lists.newLockFreeCopyOnWriteList();

    @RequiredUIAccess
    public HtmlLanguageLevelForm() {
        VerticalLayout layout = VerticalLayout.create();

        RadioGroup<HtmlOption> radioGroup = RadioGroup.create();

        myHtml4RadioButton = radioGroup.newButton(LocalizeValue.localizeTODO("HTML 4 (\"http://www.w3.org/TR/html4/loose.dtd\")"), HtmlOption.HTML4);
        myHtml5RadioButton = radioGroup.newButton(LocalizeValue.localizeTODO("HTML 5"), HtmlOption.HTML5);
        myOtherRadioButton = radioGroup.newButton(LocalizeValue.localizeTODO("Other doctype:"), HtmlOption.OTHER);

        final String[] urls = ExternalResourceManager.getInstance().getResourceUrls(null, true);
        myDoctypeTextBox = TextBoxWithHistory.create();
        myDoctypeTextBox.setHistory(List.of(urls));
        myDoctypeTextBox.setVisibleLength(48);

        layout.add(myHtml4RadioButton)
            .add(myHtml5RadioButton)
            .add(DockLayout.create().left(myOtherRadioButton).right(myDoctypeTextBox));

        myContentLayout = LabeledLayout.create(LocalizeValue.localizeTODO("Default HTML language level"), layout);

        radioGroup.addValueListener(htmlOption -> {
            myDoctypeTextBox.setEnabled(htmlOption == HtmlOption.OTHER);
            fireDoctypeChanged();
        });

        myDoctypeTextBox.addValueListener(valueEvent -> fireDoctypeChanged());
    }

    public Component getContentPanel() {
        return myContentLayout;
    }

    public String getDoctype() {
        if (myHtml4RadioButton.getValueOrError()) {
            return XmlUtil.XHTML_URI;
        }
        if (myHtml5RadioButton.getValueOrError()) {
            return Html5SchemaProvider.getHtml5SchemaLocation();
        }
        return myDoctypeTextBox.getValueOrError();
    }

    @RequiredUIAccess
    public void resetFromDoctype(final String doctype) {
        if (doctype == null || doctype.isEmpty() || doctype.equals(XmlUtil.XHTML4_SCHEMA_LOCATION)) {
            myHtml4RadioButton.setValue(true);
            myDoctypeTextBox.setEnabled(false);
        }
        else if (doctype.equals(Html5SchemaProvider.getHtml5SchemaLocation())) {
            myHtml5RadioButton.setValue(true);
            myDoctypeTextBox.setEnabled(false);
        }
        else {
            myOtherRadioButton.setValue(true);
            myDoctypeTextBox.setEnabled(true);
            myDoctypeTextBox.setValue(doctype);
        }
    }

    public void addListener(MyListener listener) {
        myListeners.add(listener);
    }

    public void removeListener(MyListener listener) {
        myListeners.remove(listener);
    }

    private void fireDoctypeChanged() {
        for (MyListener listener : myListeners) {
            listener.doctypeChanged();
        }
    }

    public interface MyListener {
        void doctypeChanged();
    }
}
