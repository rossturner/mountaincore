package technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.List;
import java.util.Map;

public class TagsWidget extends VisTable {

	private final Map<String, List<String>> sourceTags;
	private final VisTextButton addButton;

	public TagsWidget(Map<String, List<String>> sourceTags) {
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
			TagWidget tagWidget = new TagWidget(entry.getKey(), entry.getValue(), sourceTags);
			this.add(tagWidget).expandX().fillX().row();
		}

		this.add(addButton).right().row();
	}

}
