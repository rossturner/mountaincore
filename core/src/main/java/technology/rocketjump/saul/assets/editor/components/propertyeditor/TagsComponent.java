package technology.rocketjump.saul.assets.editor.components.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.List;
import java.util.Map;

public class TagsComponent extends VisTable {

	private final Map<String, List<String>> sourceTags;
	private final VisTextButton addButton;

	public TagsComponent(Map<String, List<String>> sourceTags) {
		this.sourceTags = sourceTags;

		addButton = new VisTextButton("Add another");
		addButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (!sourceTags.containsKey("")) {
					sourceTags.put("", List.of());
					reload();
				}
			}
		});

		reload();
	}

	private void reload() {
		this.clearChildren();

		for (Map.Entry<String, List<String>> entry : sourceTags.entrySet()) {
			TagComponent tagComponent = new TagComponent(entry.getKey(), entry.getValue(), sourceTags);
			this.add(tagComponent).left().expandX().row();
		}

		this.add(addButton).left().row();
	}

}
