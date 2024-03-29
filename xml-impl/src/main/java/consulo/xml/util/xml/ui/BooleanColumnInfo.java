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
package consulo.xml.util.xml.ui;

import consulo.ui.ex.awt.BooleanTableCellRenderer;
import consulo.xml.util.xml.GenericDomValue;

import javax.swing.*;
import javax.swing.table.TableCellEditor;

/**
 * @author peter
 */
public class BooleanColumnInfo extends DomColumnInfo<GenericDomValue<Boolean>, Boolean> {

  public BooleanColumnInfo(final String name) {
    super(name, new BooleanTableCellRenderer());
  }

  public TableCellEditor getEditor(GenericDomValue<Boolean> value) {
    return new DefaultCellEditor(new JCheckBox());
  }

  public final Class<Boolean> getColumnClass() {
    return Boolean.class;
  }

  public final void setValue(final GenericDomValue<Boolean> o, final Boolean aValue) {
    o.setValue(aValue);
  }

  public final Boolean valueOf(GenericDomValue<Boolean> object) {
    final Boolean value = object.getValue();
    return value == null ? Boolean.FALSE : value;
  }
}
