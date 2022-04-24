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
package com.intellij.util.xml.ui;

import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.util.lang.function.Condition;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class ComboTableCellEditor extends DefaultCellEditor {
  private final boolean myNullable;
  private final Supplier<List<Pair<String, Image>>> myDataFactory;
  private Map<String, consulo.ui.image.Image> myData;
  private static final Pair<String,Icon> EMPTY = Pair.create(" ", null);

  public ComboTableCellEditor(Supplier<List<Pair<String, consulo.ui.image.Image>>> dataFactory, final boolean nullable) {
    super(new JComboBox());
    myDataFactory = dataFactory;
    myNullable = nullable;
    setClickCountToStart(2);
    JComboBox comboBox = (JComboBox)editorComponent;
    comboBox.setBorder(null);
    comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
    ComboControl.initComboBox(comboBox, new Condition<String>() {
      public boolean value(final String object) {
        return myData != null && myData.containsKey(object) || myNullable && EMPTY.first == object;
      }
    });
  }

  public ComboTableCellEditor(Class<? extends Enum> anEnum, final boolean nullable) {
    this(ComboControl.createEnumFactory(anEnum), nullable);
  }

  public Object getCellEditorValue() {
    final Pair<String,Icon> cellEditorValue = (Pair<String,Icon>)super.getCellEditorValue();
    return EMPTY == cellEditorValue || null == cellEditorValue ? null : cellEditorValue.first;
  }

  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    final List<Pair<String, consulo.ui.image.Image>> list = myDataFactory.get();
    myData = new HashMap<>();

    final JComboBox comboBox = (JComboBox)editorComponent;
    comboBox.removeAllItems();
    if (myNullable) {
      comboBox.addItem(EMPTY);
    }
    for (final Pair<String, consulo.ui.image.Image> pair : list) {
      myData.put(pair.first, pair.second);
      comboBox.addItem(pair);
    }
    final Pair<Object, consulo.ui.image.Image> pair = Pair.create(value, myData.get(value));
    comboBox.setEditable(true);
    super.getTableCellEditorComponent(table, pair, isSelected, row, column);
    comboBox.setEditable(false);
    return comboBox;
  }
}
