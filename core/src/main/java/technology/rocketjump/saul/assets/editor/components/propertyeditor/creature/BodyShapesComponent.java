package technology.rocketjump.saul.assets.editor.components.propertyeditor.creature;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;

import java.util.List;

public class BodyShapesComponent extends VisTable {

	private final List<CreatureBodyShapeDescriptor> sourceData;
	private final VisTextButton addButton;

	public BodyShapesComponent(List<CreatureBodyShapeDescriptor> sourceData) {
		this.sourceData = sourceData;

		addButton = new VisTextButton("Add another");
		addButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				CreatureBodyShapeDescriptor bodyShapeDescriptor = new CreatureBodyShapeDescriptor();
				bodyShapeDescriptor.setValue(CreatureBodyShape.AVERAGE);
				sourceData.add(bodyShapeDescriptor);
				reload();
			}
		});

		reload();
	}


	private void reload() {
		this.clearChildren();

		for (CreatureBodyShapeDescriptor bodyShapeDescriptor : sourceData) {
			this.add(new BodyShapeComponent(bodyShapeDescriptor)).left().expandX().fillX().row();
		}

		this.add(addButton).left().row();
	}
}
