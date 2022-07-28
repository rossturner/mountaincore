package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel;
import com.kotcrab.vis.ui.widget.spinner.Spinner;
import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.saul.misc.ReflectionUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class WidgetBuilder {

	//Composite components

	public static VisTable intSpinner(String labelText, int initialValue, int minValue, int maxValue, Consumer<Integer> changeListener) {
		VisTable component = new VisTable();
		IntSpinnerModel spinnerModel = new IntSpinnerModel(initialValue, minValue, maxValue);
		Spinner spinner = new Spinner("", spinnerModel);
		spinner.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				changeListener.accept(spinnerModel.getValue());
			}
		});

		component.add(new VisLabel(niceLabel(labelText)));
		component.add(spinner);
		return component;
	}

	public static <T> VisTable checkboxGroup(String labelText, List<T> initialValue, Collection<T> options, Consumer<T> checkedListener, Consumer<T> uncheckedListener) {
		VisTable component = new VisTable();
		component.defaults().left();

		VisTable checkBoxes = new VisTable();
		for (T option : options) {
			VisCheckBox checkbox = new VisCheckBox(option.toString());
			checkbox.setChecked(initialValue.contains(option));
			checkbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (checkbox.isChecked()) {
						checkedListener.accept(option);
					} else {
						uncheckedListener.accept(option);
					}
				}
			});
			checkBoxes.add(checkbox).left().row();
		}

		component.add(new VisLabel(niceLabel(labelText)));
		component.add(checkBoxes);
		return component;
	}

	public static <T> VisTable selectField(String labelText, T initialValue, Collection<T> items, T valueIfNull, Consumer<T> changeListener) {
		VisTable visTable = new VisTable();
		VisLabel label = new VisLabel(niceLabel(labelText));
		VisSelectBox<T> selectBox = new VisSelectBox<>();
		selectBox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				changeListener.accept(selectBox.getSelected());
			}
		});

		selectBox.setItems(orderedArray(items, valueIfNull));

		if (initialValue == null) {
			if (valueIfNull != null) {
				selectBox.setSelected(valueIfNull);
			}
		} else {
			selectBox.setSelected(initialValue);
		}

		visTable.add(label).left();
		visTable.add(selectBox).left();
		return visTable;
	}

	public static VisTable slider(String labelText, int initialValue, int min, int max, int step, Consumer<Integer> changeListener) {
		VisTable spritePaddingRow = new VisTable();
		String format = "%s (%d)";
		String labelTextWithCount = String.format(format, labelText, initialValue);

		VisLabel label = new VisLabel(niceLabel(labelTextWithCount));
		spritePaddingRow.add(label).left();
		VisSlider slider = new VisSlider(min, max, step, false);
		slider.setValue(initialValue);
		slider.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int newValue = (int) slider.getValue();
				label.setText(niceLabel(String.format(format, labelText, newValue)));
				changeListener.accept(newValue);
			}
		});
		spritePaddingRow.add(slider).left();
		return spritePaddingRow;
	}

	//Reflective Java Bean components
	public static void addTextField(String labelText, String propertyName, Object instance, VisTable table) {
		VisTextField textField = new VisTextField(ReflectionUtils.getProperty(instance, propertyName).toString());
		textField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				ReflectionUtils.setProperty(instance, propertyName, textField.getText());
			}
		});
		table.add(new VisLabel(labelText)).left();
		table.add(textField).left().expandX().fillX().row();
	}

	public static void addIntegerField(String labelText, String propertyName, Object instance, VisTable table) {
		VisTextField textField = new VisTextField(String.valueOf(ReflectionUtils.getProperty(instance, propertyName)));
		textField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					Integer newValue = Integer.valueOf(textField.getText());
					ReflectionUtils.setProperty(instance, propertyName, newValue);
				} catch (NumberFormatException e) {
				}
			}
		});
		table.add(new VisLabel(labelText)).left();
		table.add(textField).left().expandX().fillX().row();
	}

	public static void addFloatField(String labelText, String propertyName, Object instance, VisTable table) {
		VisTextField textField = new VisTextField(String.valueOf(ReflectionUtils.getProperty(instance, propertyName)));
		textField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					Float newValue = Float.valueOf(textField.getText());
					ReflectionUtils.setProperty(instance, propertyName, newValue);
				} catch (NumberFormatException e) {
					ReflectionUtils.setProperty(instance, propertyName, null);
				}
			}
		});
		table.add(new VisLabel(labelText)).left();
		table.add(textField).left().expandX().fillX().row();
	}


	public static <T> void addSelectField(String labelText, String propertyName, Collection<T> items, T valueIfNull,
										  Object instance, VisTable table) {
		T initialValue = (T) ReflectionUtils.getProperty(instance, propertyName);
		Consumer<T> reflectionPropertySetter = selected -> ReflectionUtils.setProperty(instance, propertyName, selected);

		VisTable visTable = selectField(labelText, initialValue, items, valueIfNull, reflectionPropertySetter);
//		visTable.getChildren().forEach(child -> table.add(child).left());
		table.add(visTable).colspan(2).left().row();
//		table.row();
	}

	public static <T> Array<T> orderedArray(Collection<T> items) {
		return orderedArray(items, null);
	}

	public static <T> Array<T> orderedArray(Collection<T> items, T nullItem) {
		Array<T> array = new Array<>();
		items.forEach(array::add);
		array.sort(Comparator.comparing(Object::toString));


		if (nullItem != null) {
			if (items.stream().noneMatch(item -> item.toString().equals(nullItem.toString()))) {
				array.insert(0, nullItem);
			}
		}
		return array;
	}


	private static String niceLabel(String labelText) {
		return WordUtils.capitalizeFully(labelText.endsWith(":") ? labelText : labelText + ":", '_');
	}

}
