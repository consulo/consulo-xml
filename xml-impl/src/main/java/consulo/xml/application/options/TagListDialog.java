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
package consulo.xml.application.options;

import consulo.application.localize.ApplicationLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class TagListDialog extends DialogWrapper {
    private final JPanel myPanel;
    private final JList<String> myList = new JBList<>(new DefaultListModel<>());
    private ArrayList<String> myData;

    public TagListDialog(String title) {
        super(true);
        myPanel = ToolbarDecorator.createDecorator(myList)
            .setAddAction(button -> {
                final String tagName = Messages.showInputDialog(
                    ApplicationLocalize.editboxEnterTagName().get(),
                    ApplicationLocalize.titleTagName().get(),
                    UIUtil.getQuestionIcon()
                );
                if (tagName != null) {
                    while (myData.contains(tagName)) {
                        myData.remove(tagName);
                    }
                    myData.add(tagName);
                    updateData();
                    myList.setSelectedIndex(myData.size() - 1);
                }
            })
            .setRemoveAction(button -> {
                int selectedIndex = myList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    myData.remove(selectedIndex);
                    updateData();
                    if (selectedIndex >= myData.size()) {
                        selectedIndex -= 1;
                    }
                    if (selectedIndex >= 0) {
                        myList.setSelectedIndex(selectedIndex);
                    }
                }
            })
            .disableUpDownActions()
            .createPanel();
        setTitle(title);
        init();
    }

    public void setData(ArrayList<String> data) {
        myData = data;
        updateData();
        if (!myData.isEmpty()) {
            myList.setSelectedIndex(0);
        }
    }

    private void updateData() {
        DefaultListModel<String> model = (DefaultListModel<String>)myList.getModel();
        model.clear();
        for (String data : myData) {
            model.addElement(data);
        }
    }

    public List<String> getData() {
        return myData;
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myList;
    }
}
