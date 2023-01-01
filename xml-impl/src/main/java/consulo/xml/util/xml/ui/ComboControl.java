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
package consulo.xml.util.xml.ui;

import consulo.application.ApplicationManager;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.highlighting.DomElementAnnotationsManager;
import consulo.xml.util.xml.highlighting.DomElementProblemDescriptor;
import consulo.xml.util.xml.highlighting.DomElementsProblemsHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

;

/**
 * @author peter
 */
public class ComboControl extends BaseModifiableControl<JComboBox, String> {
  private static final Pair<String, Image> EMPTY = new ComboBoxItem(" ", null);
  private final Supplier<List<Pair<String, Image>>> myDataFactory;
  private boolean myNullable;
  private final Map<String, Image> myIcons = new HashMap<>();
  private final ItemListener myCommitListener = new ItemListener() {
    public void itemStateChanged(ItemEvent e) {
      setModified();
      commit();
    }
  };

  public ComboControl(final GenericDomValue genericDomValue, final Supplier<List<Pair<String, Image>>> dataFactory) {
    this(new DomStringWrapper(genericDomValue), dataFactory);
  }

  public ComboControl(final DomWrapper<String> domWrapper, final Supplier<List<Pair<String, Image>>> dataFactory) {
    super(domWrapper);
    myDataFactory = dataFactory;
    reset();
  }

  public ComboControl(final DomWrapper<String> domWrapper, final Class<? extends Enum> aClass) {
    super(domWrapper);
    myDataFactory = createEnumFactory(aClass);
    reset();
  }

  public final boolean isNullable() {
    return myNullable;
  }

  public final void setNullable(final boolean nullable) {
    myNullable = nullable;
  }

  public ComboControl(final GenericDomValue<?> reference) {
    this(reference, createResolvingFunction(reference));
  }

  public static Supplier<List<Pair<String, Image>>> createResolvingFunction(final GenericDomValue<?> reference) {
    return new Supplier<List<Pair<String, Image>>>() {
      public List<Pair<String, Image>> get() {
        final Converter converter = reference.getConverter();
        if (converter instanceof ResolvingConverter) {
          final AbstractConvertContext context = new AbstractConvertContext() {
            @Nonnull
            public DomElement getInvocationElement() {
              return reference;
            }
          };
          final ResolvingConverter<?> resolvingConverter = (ResolvingConverter) converter;
          final Collection<?> variants = resolvingConverter.getVariants(context);
          final List<Pair<String, Image>> all = ContainerUtil.map(variants, new Function<Object, Pair<String, Image>>() {
            public Pair<String, Image> apply(final Object s) {
              return Pair.create(ElementPresentationManager.getElementName(s), ElementPresentationManager.getIcon(s));
            }
          });
          all.addAll(ContainerUtil.map(resolvingConverter.getAdditionalVariants(context), s -> Pair.<String, Image>create(s, null)));
          return all;
        }
        return Collections.emptyList();
      }
    };
  }

  public static Supplier<List<Pair<String, Image>>> createPresentationFunction(final Supplier<Collection<? extends Object>> variantFactory) {
    return new Supplier<List<Pair<String, Image>>>() {
      public List<Pair<String, Image>> get() {
        return ContainerUtil.map(variantFactory.get(), s -> Pair.create(ElementPresentationManager.getElementName(s), ElementPresentationManager.getIcon(s)));
      }
    };
  }

  static Supplier<List<Pair<String, Image>>> createEnumFactory(final Class<? extends Enum> aClass) {
    return new Supplier<List<Pair<String, Image>>>() {
      public List<Pair<String, Image>> get() {
        return ContainerUtil.map2List(aClass.getEnumConstants(), s -> Pair.create(NamedEnumUtil.getEnumValueByElement(s), ElementPresentationManager.getIcon(s)));
      }
    };
  }

  public static <T extends Enum> JComboBox createEnumComboBox(final Class<T> type) {
    return tuneUpComboBox(new JComboBox(), createEnumFactory(type));
  }

  private static JComboBox tuneUpComboBox(final JComboBox comboBox, Supplier<List<Pair<String, Image>>> dataFactory) {
    final List<Pair<String, Image>> list = dataFactory.get();
    final Set<String> standardValues = new HashSet<>();
    for (final Pair<String, Image> pair : list) {
      comboBox.addItem(new ComboBoxItem(pair));
      standardValues.add(pair.first);
    }
    return initComboBox(comboBox, new Condition<String>() {
      public boolean value(final String object) {
        return standardValues.contains(object);
      }
    });
  }

  private static class ComboBoxItem extends Pair<String, Image> {

    public ComboBoxItem(String first, Image second) {
      super(first, second);
    }

    public ComboBoxItem(Pair<String, Image> pair) {
      super(pair.first, pair.second);
    }

    public String toString() {
      return StringUtil.notNullize(first);
    }
  }

  static JComboBox initComboBox(final JComboBox comboBox, final Condition<String> validity) {
    comboBox.setEditable(false);
    comboBox.setPrototypeDisplayValue(new ComboBoxItem("A", null));
    comboBox.setRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        final Pair<String, Icon> pair = (Pair<String, Icon>) value;
        final String text = pair == null ? null : pair.first;
        setText(text);
        final Dimension dimension = getPreferredSize();
        if (!validity.value(text)) {
          setFont(getFont().deriveFont(Font.ITALIC));
          setForeground(JBColor.RED);
        }
        setIcon(pair == null ? null : pair.second);
        setPreferredSize(new Dimension(-1, dimension.height));
        return this;
      }
    });
    return comboBox;
  }

  protected JComboBox createMainComponent(final JComboBox boundedComponent) {
    return initComboBox(boundedComponent == null ? new JComboBox() : boundedComponent, new Condition<String>() {
      public boolean value(final String object) {
        return isValidValue(object);
      }
    });
  }

  public boolean isValidValue(final String object) {
    return myNullable && object == EMPTY.first || myIcons.containsKey(object);
  }

  private boolean dataChanged(List<Pair<String, Image>> newData) {
    final JComboBox comboBox = getComponent();
    final int size = comboBox.getItemCount();
    final List<Pair<String, Image>> oldData = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      oldData.add((Pair<String, Image>) comboBox.getItemAt(i));
    }

    if (myNullable) {
      final LinkedList<Pair<String, Image>> list = new LinkedList<>(newData);
      list.addFirst(EMPTY);
      newData = list;
    }

    return !newData.equals(oldData);
  }

  protected boolean isCommitted() {
    return getComponent().isPopupVisible() || super.isCommitted();
  }

  protected void doReset() {
    final List<Pair<String, Image>> data = myDataFactory.get();
    final JComboBox comboBox = getComponent();
    comboBox.removeItemListener(myCommitListener);
    try {
      if (!dataChanged(data)) {
        super.doReset();
        return;
      }

      final String oldValue = getValue();
      myIcons.clear();
      comboBox.removeAllItems();
      if (myNullable) {
        comboBox.addItem(EMPTY);
      }
      for (final Pair<String, Image> s : data) {
        comboBox.addItem(new ComboBoxItem(s));
        myIcons.put(s.first, s.second);
      }
      setValue(oldValue);
      super.doReset();
    } finally {
      comboBox.addItemListener(myCommitListener);
    }
  }

  @Nullable
  public final String getValue() {
    final Pair<String, Image> pair = (Pair<String, Image>) getComponent().getSelectedItem();
    return pair == null || pair == EMPTY ? null : pair.first;
  }

  public final void setValue(final String value) {
    final JComboBox component = getComponent();
    if (!isValidValue(value)) {
      component.setEditable(true);
    }
    component.setSelectedItem(new ComboBoxItem(value, myIcons.get(value)));
    component.setEditable(false);
  }


  protected void updateComponent() {
    final DomElement domElement = getDomElement();
    if (domElement == null || !domElement.isValid()) return;

    final JComboBox comboBox = getComponent();

    final Project project = getProject();
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        if (!project.isOpen()) return;
        if (!getDomWrapper().isValid()) return;

        final DomElement domElement = getDomElement();
        if (domElement == null || !domElement.isValid()) return;

        final DomElementAnnotationsManager manager = DomElementAnnotationsManager.getInstance(project);
        final DomElementsProblemsHolder holder = manager.getCachedProblemHolder(domElement);
        final List<DomElementProblemDescriptor> errorProblems = holder.getProblems(domElement);
        final List<DomElementProblemDescriptor> warningProblems = holder.getProblems(domElement, true, HighlightSeverity.WARNING);

        Color background = getDefaultBackground();
        comboBox.setToolTipText(null);

        if (errorProblems.size() > 0) {
          background = getErrorBackground();
          comboBox.setToolTipText(TooltipUtils.getTooltipText(errorProblems));
        } else if (warningProblems.size() > 0) {
          background = getWarningBackground();
          comboBox.setToolTipText(TooltipUtils.getTooltipText(warningProblems));
        }

        final Pair<String, Icon> pair = (Pair<String, Icon>) comboBox.getSelectedItem();
        final String s = pair == null ? null : pair.first;
        background = s != null && s.trim().length() > 0 ? getDefaultBackground() : background;

        comboBox.setBackground(background);
        comboBox.getEditor().getEditorComponent().setBackground(background);
      }
    });

  }
}
