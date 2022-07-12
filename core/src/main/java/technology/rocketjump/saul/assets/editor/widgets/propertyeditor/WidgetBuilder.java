package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import technology.rocketjump.saul.misc.ReflectionUtils;

import java.util.Collection;
import java.util.Comparator;

public class WidgetBuilder {

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
		VisSelectBox<T> selectBox = new VisSelectBox<>();
		selectBox.setItems(orderedArray(items, valueIfNull));
		T initialValue = (T) ReflectionUtils.getProperty(instance, propertyName);
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
				ReflectionUtils.setProperty(instance, propertyName, selectBox.getSelected());
			}
		});
		table.add(new VisLabel(labelText)).left();
		table.add(selectBox).left().row();
	}


	public static <T> Array<T> orderedArray(Collection<T> items) {
		return orderedArray(items, null);
	}

	public static <T> Array<T> orderedArray(Collection<T> items, T nullItem) {
		Array<T> array = new Array<>();
		items.forEach(array::add);
		array.sort(Comparator.comparing(Object::toString));
		if (nullItem != null) {
			array.insert(0, nullItem);
		}
		return array;
	}

}
