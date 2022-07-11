package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature;

import com.kotcrab.vis.ui.widget.VisTable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;

import java.util.List;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.addFloatField;
import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.addSelectField;

public class BodyShapeWidget extends VisTable {

	public BodyShapeWidget(CreatureBodyShapeDescriptor bodyShapeDescriptor) {
		try {
			addSelectField("Value:", "value", List.of(CreatureBodyShape.values()),
					CreatureBodyShape.AVERAGE, bodyShapeDescriptor, this);

			addFloatField("Min Strength:", "minStrength", bodyShapeDescriptor, this);
			addFloatField("Max Strength:", "maxStrength", bodyShapeDescriptor, this);

			this.addSeparator().colspan(2).row();
		} catch (WidgetBuilder.PropertyReflectionException e) {
			Logger.error("Error with reflection in " + getClass().getSimpleName(), e);
		}


	}
}
