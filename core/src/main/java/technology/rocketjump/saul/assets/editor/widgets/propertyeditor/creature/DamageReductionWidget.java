package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import technology.rocketjump.saul.entities.model.physical.combat.CombatDamageType;

import java.util.Map;

public class DamageReductionWidget extends VisTable {
	public DamageReductionWidget(Map<CombatDamageType, Integer> sourceData) {

		for (CombatDamageType damageType : CombatDamageType.values()) {
			this.add(new VisLabel(damageType.name())).left();

			VisTextField textField = new VisTextField();
			if (sourceData.containsKey(damageType)) {
				textField.setText(sourceData.get(damageType).toString());
			}
			textField.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					try {
						if (textField.getText().equals("")) {
							sourceData.remove(damageType);
						} else {
							Integer newValue = Integer.parseInt(textField.getText());
							sourceData.put(damageType, newValue);
						}
					} catch (NumberFormatException ignored) {

					}
				}
			});
			this.add(textField).left().row();
		}

	}
}
