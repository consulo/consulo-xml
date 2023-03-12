package consulo.xml.codeInspection.htmlInspections;

import consulo.configurable.ConfigurableBuilder;
import consulo.configurable.ConfigurableBuilderState;
import consulo.configurable.UnnamedConfigurable;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.localize.LocalizeValue;
import consulo.ui.Label;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Transient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 11/03/2023
 */
public abstract class BaseXmlEntitiesInspectionState<E> implements InspectionToolState<E> {
  protected static final Function<String, List<String>> PARSER = s -> StringUtil.split(s, ",");
  protected static final Function<List<String>, String> JOINER = strings -> String.join(",", strings);

  private String[] myEntities;

  @Transient
  protected final LocalizeValue myLabelText;

  // only for serialize
  public BaseXmlEntitiesInspectionState() {
    myLabelText = LocalizeValue.of();
    myEntities = new String[0];
  }

  public BaseXmlEntitiesInspectionState(@Nonnull LocalizeValue labelText) {
    myLabelText = labelText;
    myEntities = new String[0];
  }

  public BaseXmlEntitiesInspectionState(@Nonnull LocalizeValue labelText, String... defaultValues) {
    myLabelText = labelText;
    myEntities = defaultValues;
  }

  public void addEntry(String entity) {
    myEntities = ArrayUtil.append(myEntities, entity);
  }

  public String[] getEntities() {
    return myEntities;
  }

  public void setEntities(String[] entities) {
    myEntities = entities;
  }

  public boolean containsEntity(String value) {
    for (String entity : myEntities) {
      if (value.equalsIgnoreCase(entity)) {
        return true;
      }
    }

    return false;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public E getState() {
    return (E)this;
  }

  @Override
  public void loadState(E state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Nullable
  @Override
  public UnnamedConfigurable createConfigurable() {
    ConfigurableBuilder<ConfigurableBuilderState> newBuilder = ConfigurableBuilder.newBuilder();
    newBuilder.component(Label.create(myLabelText));
    newBuilder.textBoxWithExpandAction(null, myLabelText.get(), PARSER, JOINER, () -> JOINER.apply(Arrays.asList(getEntities())), s -> {
      List<String> values = PARSER.apply(s);
      setEntities(ArrayUtil.toStringArray(values));
    });
    return newBuilder.buildUnnamed();
  }
}
