package technology.rocketjump.saul.assets.editor.components.propertyeditor.creature;

import com.kotcrab.vis.ui.widget.VisTable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static technology.rocketjump.saul.assets.editor.components.propertyeditor.ComponentBuilder.addFloatField;
import static technology.rocketjump.saul.assets.editor.components.propertyeditor.ComponentBuilder.addSelectField;

public class BodyShapeComponent extends VisTable {

	public BodyShapeComponent(CreatureBodyShapeDescriptor bodyShapeDescriptor) {
		try {
			addSelectField("Value:", "value", List.of(CreatureBodyShape.values()),
					CreatureBodyShape.AVERAGE, bodyShapeDescriptor, this);

			addFloatField("Min Strength:", "minStrength", bodyShapeDescriptor, this);
			addFloatField("Max Strength:", "maxStrength", bodyShapeDescriptor, this);

			this.addSeparator().colspan(2).row();
		} catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
			Logger.error("Error with reflection in " + getClass().getSimpleName(), e);
		}


	}
}
