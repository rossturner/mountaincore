package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature.DamageReductionWidget;
import technology.rocketjump.saul.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.saul.entities.model.physical.combat.DefenseType;

import java.util.List;

public class DefenseInfoWidget extends VisTable {

	public DefenseInfoWidget(DefenseInfo defenseInfo) {
		this.columnDefaults(0).uniformX().left();
		this.columnDefaults(1).fillX().left();

		WidgetBuilder.addSelectField("Type:", "type", List.of(DefenseType.values()), DefenseInfo.NONE.getType(), defenseInfo, this);
		WidgetBuilder.addIntegerField("Max Defense Points:", "maxDefensePoints", defenseInfo, this);
		WidgetBuilder.addIntegerField("Max Defense Regained/Round:", "maxDefenseRegainedPerRound", defenseInfo, this);

		this.add(new VisLabel("Damage reduction: (integer)")).left().colspan(2).row();
		this.add(new DamageReductionWidget(defenseInfo.getDamageReduction())).left().colspan(2).row();
	}

}
