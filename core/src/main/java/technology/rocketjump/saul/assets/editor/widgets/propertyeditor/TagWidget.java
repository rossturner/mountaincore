package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagWidget extends VisTable {

	private String key;
	private ArrayList<String> values;
	private Map<String, List<String>> sourceMap;

	private final VisTextField keyField;

	public TagWidget(String key, List<String> values, Map<String, List<String>> sourceMap) {
		this.key = key;
		this.values = new ArrayList<>(values);
		this.sourceMap = sourceMap;


		keyField = new VisTextField(key);
		keyField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String newKey = keyField.getText();
				sourceMap.remove(TagWidget.this.key);
				sourceMap.put(newKey, TagWidget.this.values);
				TagWidget.this.key = newKey;
			}
		});

		reload();
	}

	private void reload() {
		this.clearChildren();

		this.add(keyField).left().fillX().row();

		for (int index = 0; index < values.size(); index++) {
			String value = index == values.size() ? "" : values.get(index);
			VisTextField valueField = new VisTextField(value);
			final int Index = index;
			valueField.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					String newValue = valueField.getText();
					values.set(Index, newValue);
					sourceMap.put(key, values);
				}
			});
			this.add(valueField).padLeft(40).fillX().row();
		}

		VisTextButton addValueButton = new VisTextButton("Add tag value");
		addValueButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				values.add("");
				reload();
			}
		});
		this.add(addValueButton).right().row();

		this.addSeparator().row();
	}
}
