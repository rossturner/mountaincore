package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.spinner.FloatSpinnerModel;
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel;
import com.kotcrab.vis.ui.widget.spinner.Spinner;
import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.saul.assets.editor.model.ColorPickerMessage;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.ReflectionUtils;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WidgetBuilder {

	//Singular components
	public static VisLabel label(String labelText) {
		return new VisLabel(niceLabel(labelText));
	}

	public static VisValidatableTextField textField(String initialValue, Consumer<String> changeListener, InputValidator... validators) {
		VisValidatableTextField textField = new VisValidatableTextField();
		textField.setText(initialValue);
		textField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				changeListener.accept(textField.getText());
			}
		});
		for (InputValidator validator : validators) {
			textField.addValidator(validator);
		}
		return textField;
	}

	public static <T> VisSelectBox<T> select(T initialValue, T[] items, T valueIfNull, Consumer<T> changeListener) {
		return select(initialValue, Arrays.asList(items), valueIfNull, changeListener);
	}

	public static <T> VisSelectBox<T> select(T initialValue, Collection<T> items, T valueIfNull, Consumer<T> changeListener) {
		VisSelectBox<T> selectBox = new VisSelectBox<>();
		selectBox.setItems(orderedArray(items, valueIfNull));
		if (initialValue == null) {
			if (valueIfNull != null) {
				selectBox.setSelected(valueIfNull);
			}
		} else {
			selectBox.setSelected(initialValue);
		}
		selectBox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				changeListener.accept(selectBox.getSelected());
			}
		});
		return selectBox;
	}

	public static Spinner intSpinner(int initialValue, int minValue, int maxValue, Consumer<Integer> changeListener) {
		IntSpinnerModel spinnerModel = new IntSpinnerModel(initialValue, minValue, maxValue);
		Spinner spinner = new Spinner("", spinnerModel);
		spinner.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				changeListener.accept(spinnerModel.getValue());
			}
		});
		return spinner;
	}

	public static Spinner floatSpinner(float initialValue, float minValue, float maxValue, Consumer<Float> changeListener) {
		return floatSpinner(initialValue, minValue, maxValue, changeListener, 1.0f, 1);
	}

	public static Spinner floatSpinner(float initialValue, float minValue, float maxValue, Consumer<Float> changeListener, float step, int scale) {
		FloatSpinnerModel spinnerModel = new FloatSpinnerModel(String.valueOf(initialValue), String.valueOf(minValue), String.valueOf(maxValue), String.valueOf(step), scale);
		Spinner spinner = new Spinner("", spinnerModel);
		spinner.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				changeListener.accept(spinnerModel.getValue().floatValue());
			}
		});
		return spinner;
	}

	public static Spinner doubleSpinner(double initialValue, double minValue, double maxValue, Consumer<Double> changeListener) {
		FloatSpinnerModel spinnerModel = new FloatSpinnerModel(String.valueOf(initialValue), String.valueOf(minValue), String.valueOf(maxValue));
		Spinner spinner = new Spinner("", spinnerModel);
		spinner.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				changeListener.accept(spinnerModel.getValue().doubleValue());
			}
		});
		return spinner;
	}

	public static VisTextButton button(String buttonText, Consumer<TextButton> changeListener) {
		VisTextButton visTextButton = new VisTextButton(buttonText);
		visTextButton.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				changeListener.accept(visTextButton);
				return true;
			}
		});
		return visTextButton;
	}

	public static VisTextButton toggle(boolean initialValue, Consumer<Boolean> changeListener) {
		AtomicBoolean atomicBoolean = new AtomicBoolean(initialValue);
		VisTextButton toggle = new VisTextButton(String.valueOf(initialValue));
		toggle.setColor(initialValue ? Color.GREEN : Color.RED);
		toggle.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				boolean newValue = !atomicBoolean.get();
				atomicBoolean.set(newValue);
				toggle.setText(String.valueOf(newValue));
				toggle.setColor(newValue ? Color.GREEN : Color.RED);
				changeListener.accept(newValue);
			}
		});

		return toggle;
	}

	public static <T> VisCheckBox checkBox(T option, boolean initialValue, Consumer<T> checkedListener, Consumer<T> uncheckedListener) {
		VisCheckBox checkbox = new VisCheckBox(option.toString());
		checkbox.setChecked(initialValue);
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
		return checkbox;
	}


	//Composite components
	public static VisTable colorPickerTextField(MessageDispatcher messageDispatcher, Color initialColor, BiConsumer<Color, String> changeListener) {
		VisTable component = new VisTable();

		VisTextField colorCodeField = new VisTextField();
		VisTextField.VisTextFieldStyle colorCodeStyle = new VisTextField.VisTextFieldStyle(colorCodeField.getStyle());
		colorCodeStyle.fontColor = initialColor;
		colorCodeField.setStyle(colorCodeStyle);
		colorCodeField.setText(HexColors.toHexString(initialColor));
		colorCodeField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Color validatedColor = HexColors.get(colorCodeField.getText());
				if (validatedColor != null) {
					changeListener.accept(validatedColor, HexColors.toHexString(validatedColor));
					colorCodeStyle.fontColor = validatedColor;
				} else {
					changeListener.accept(null, null);
				}
			}
		});

		VisTextButton colorPickerButton = new VisTextButton("Picker");
		colorPickerButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.EDITOR_SHOW_COLOR_PICKER,
						new ColorPickerMessage(colorCodeField.getText(), (color) -> {
							changeListener.accept(color, HexColors.toHexString(color));
							colorCodeStyle.fontColor = color;
							colorCodeField.setText(HexColors.toHexString(color));
						}));
			}
		});

		component.add(colorCodeField).expandX().fillX();
		component.add(colorPickerButton);
		return component;
	}


	public static VisTable textField(String labelText, String initialValue, Consumer<String> changeListener, InputValidator inputValidator) {
		VisValidatableTextField textField = textField(initialValue, changeListener, inputValidator);
		VisTable component = new VisTable();
		component.add(label(labelText));
		component.add(textField);
		return component;
	}

	public static VisTable intSpinner(String labelText, int initialValue, int minValue, int maxValue, Consumer<Integer> changeListener) {
		VisTable component = new VisTable();
		component.add(label(labelText));
		component.add(intSpinner(initialValue, minValue, maxValue, changeListener));
		return component;
	}

	public static <T> VisTable checkboxGroup(String labelText, List<T> initialValue, Collection<T> options, Consumer<T> checkedListener, Consumer<T> uncheckedListener) {
		VisTable component = new VisTable();
		component.defaults().left();
		VisTable checkBoxes = checkboxes(initialValue, options, checkedListener, uncheckedListener);
		component.add(label(labelText));
		component.add(checkBoxes);
		return component;
	}

	public static <T> VisTable checkboxes(Collection<T> initialValue, Collection<T> options, Consumer<T> checkedListener, Consumer<T> uncheckedListener) {
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
			checkBoxes.add(checkbox).expandX().left().row();
		}
		return checkBoxes;
	}

	public static <T> VisTable selectField(String labelText, T initialValue, Collection<T> items, T valueIfNull, Consumer<T> changeListener) {
		VisTable visTable = new VisTable();
		VisLabel label = label(labelText);
		VisSelectBox<T> selectBox = select(initialValue, items, valueIfNull, changeListener);

		visTable.add(label).left();
		visTable.add(selectBox).left();
		return visTable;
	}

	public static VisTable slider(String labelText, int initialValue, int min, int max, int step, Consumer<Integer> changeListener) {
		VisTable spritePaddingRow = new VisTable();
		String format = "%s (%d)";
		String labelTextWithCount = String.format(format, labelText, initialValue);

		VisLabel label = label(labelTextWithCount);
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
		table.add(label(labelText)).left();
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
		table.add(label(labelText)).left();
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
					ReflectionUtils.setProperty(instance, propertyName, 0f);
				}
			}
		});
		table.add(label(labelText)).left();
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

	public static <T extends Widget> Optional<T> findFirst(Group group, Class<T> type) {
		for (Actor child : group.getChildren()) {
			if (type.isInstance(child)) {
				return Optional.of((T) child);
			}
		}
		return Optional.empty();
	}
}
