package consulo.xml.codeInspection.htmlInspections;

import consulo.configurable.ConfigurableBuilder;
import consulo.configurable.ConfigurableBuilderState;
import consulo.configurable.UnnamedConfigurable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 11/03/2023
 */
public abstract class BaseHtmlEntitiesInspectionState<E> extends BaseXmlEntitiesInspectionState<E> {
  private static class HtmlEntitiesConfigurableState implements ConfigurableBuilderState {
    private CheckBox myCustomValuesBox;
    private TextBox myValuesBox;

    @RequiredUIAccess
    @Override
    public void uiCreated() {
      myValuesBox.setEnabled(false);
      myCustomValuesBox.addValueListener(valueEvent -> myValuesBox.setEnabled(valueEvent.getValue()));
    }
  }

  private boolean myCustomValuesEnabled = true;

  public BaseHtmlEntitiesInspectionState() {
  }

  public BaseHtmlEntitiesInspectionState(@Nonnull LocalizeValue labelText) {
    super(labelText);
  }

  public BaseHtmlEntitiesInspectionState(@Nonnull LocalizeValue labelText, String... defaultValues) {
    super(labelText, defaultValues);
  }

  public void setCustomValuesEnabled(boolean customValuesEnabled) {
    myCustomValuesEnabled = customValuesEnabled;
  }

  public boolean isCustomValuesEnabled() {
    return myCustomValuesEnabled;
  }

  @Override
  public void addEntry(String entity) {
    super.addEntry(entity);

    setCustomValuesEnabled(true);
  }

  @Nullable
  @Override
  public UnnamedConfigurable createConfigurable() {
    ConfigurableBuilder<HtmlEntitiesConfigurableState> newBuilder = ConfigurableBuilder.newBuilder(HtmlEntitiesConfigurableState::new);
    newBuilder.checkBox(myLabelText,
                        this::isCustomValuesEnabled,
                        this::setCustomValuesEnabled,
                        (state, checkBox) -> state.myCustomValuesBox = checkBox);
    newBuilder.textBoxWithExpandAction(null, myLabelText.get(), PARSER, JOINER, () -> {
      return JOINER.apply(Arrays.asList(getEntities()));
    }, s -> {
      List<String> values = PARSER.apply(s);
      setEntities(ArrayUtil.toStringArray(values));
    }, (state, box) -> state.myValuesBox = box);
    return newBuilder.buildUnnamed();
  }
}
