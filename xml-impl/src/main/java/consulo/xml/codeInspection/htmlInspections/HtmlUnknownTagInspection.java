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

package consulo.xml.codeInspection.htmlInspections;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.util.Function;
import consulo.ui.ex.awt.FieldPanel;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public abstract class HtmlUnknownTagInspection extends HtmlUnknownTagInspectionBase {

  public HtmlUnknownTagInspection() {
    super();
  }

  public HtmlUnknownTagInspection(@NonNls @Nonnull final String defaultValues) {
    super(defaultValues);
  }

  @Override
  @Nullable
  public JComponent createOptionsPanel() {
    return createOptionsPanel(this);
  }

  @Nonnull
  protected static JComponent createOptionsPanel(@Nonnull final HtmlUnknownElementInspection inspection) {
    final JPanel result = new JPanel(new BorderLayout());

    final JPanel internalPanel = new JPanel(new BorderLayout());
    result.add(internalPanel, BorderLayout.NORTH);

    final Ref<FieldPanel> panelRef = new Ref<FieldPanel>();
    final FieldPanel additionalAttributesPanel = new FieldPanel(null, null, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        Messages.showTextAreaDialog(panelRef.get().getTextField(), consulo.ide.impl.idea.openapi.util.text.StringUtil.wordsToBeginFromUpperCase(inspection.getPanelTitle()), inspection.getClass().getSimpleName(),
            new Function<String, List<String>>() {
              @Override
              public List<String> fun(String s) {
                return reparseProperties(s);
              }
            }, new Function<List<String>, String>() {
              @Override
              public String fun(List<String> strings) {
                return StringUtil.join(strings, ",");
              }
            }
        );
      }
    }, null);
    ((JButton) additionalAttributesPanel.getComponent(1)).setIcon(TargetAWT.to(AllIcons.Actions.ShowViewer));
    panelRef.set(additionalAttributesPanel);
    additionalAttributesPanel.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        final Document document = e.getDocument();
        try {
          final String text = document.getText(0, document.getLength());
          if (text != null) {
            inspection.updateAdditionalEntries(text.trim());
          }
        } catch (BadLocationException e1) {
          inspection.getLogger().error(e1);
        }
      }
    });

    final JCheckBox checkBox = new JCheckBox(inspection.getCheckboxTitle());
    checkBox.setSelected(inspection.isCustomValuesEnabled());
    checkBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final boolean b = checkBox.isSelected();
        if (b != inspection.isCustomValuesEnabled()) {
          inspection.enableCustomValues(b);
          additionalAttributesPanel.setEnabled(inspection.isCustomValuesEnabled());
        }
      }
    });

    internalPanel.add(checkBox, BorderLayout.NORTH);
    internalPanel.add(additionalAttributesPanel, BorderLayout.CENTER);

    additionalAttributesPanel.setPreferredSize(new Dimension(150, additionalAttributesPanel.getPreferredSize().height));
    additionalAttributesPanel.setEnabled(inspection.isCustomValuesEnabled());
    additionalAttributesPanel.setText(inspection.getAdditionalEntries());

    return result;
  }
}
