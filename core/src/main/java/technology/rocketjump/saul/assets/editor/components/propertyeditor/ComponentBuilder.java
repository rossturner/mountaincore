package technology.rocketjump.saul.assets.editor.components.propertyeditor;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Comparator;

public class ComponentBuilder {

	public static ShapeRenderer staticShapeRenderer; // needs initialising

	public static void addTextField(String labelText, String propertyName, Object instance, VisTable table) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		VisTextField textField = new VisTextField(PropertyUtils.getProperty(instance, propertyName).toString());
		textField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					PropertyUtils.setProperty(instance, propertyName, textField.getText());
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {

				}
			}
		});
		table.add(new VisLabel(labelText)).left();
		table.add(textField).left().expandX().fillX().row();
	}

	public static void addFloatField(String labelText, String propertyName, Object instance, VisTable table) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		VisTextField textField = new VisTextField(String.valueOf(PropertyUtils.getProperty(instance, propertyName)));
		textField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

				try {
					Float newValue = Float.valueOf(textField.getText());
					if (newValue != null) {
						PropertyUtils.setProperty(instance, propertyName, newValue);
					}
				} catch (NumberFormatException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {

				}
			}
		});
		table.add(new VisLabel(labelText)).left();
		table.add(textField).left().expandX().fillX().row();
	}

	public static <T> void addSelectField(String labelText, String propertyName, Collection<T> items, T valueIfNull,
										  Object instance, VisTable table) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		VisSelectBox<T> selectBox = new VisSelectBox<>();
		selectBox.setItems(orderedArray(items));
		T initialValue = (T) PropertyUtils.getProperty(instance, propertyName);
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
				try {
					PropertyUtils.setProperty(instance, propertyName, selectBox.getSelected());
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {

				}
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
