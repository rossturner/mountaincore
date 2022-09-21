package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;

import java.util.List;

public class RestrictedByRaceWidget extends VisTable {

	private final List<String> sourceData;
	private final RaceDictionary raceDictionary;

	public RestrictedByRaceWidget(List<String> sourceData, RaceDictionary raceDictionary) {
		this.sourceData = sourceData;
		this.raceDictionary = raceDictionary;

		reload();
	}

	private void reload() {
		this.clearChildren();

		for (String raceName : sourceData) {
			VisSelectBox<Race> raceSelect = WidgetBuilder.select(raceDictionary.getByName(raceName), raceDictionary.getAll(), null, otherRace -> {
				sourceData.remove(raceName);
				sourceData.add(otherRace.getName());
			});
			this.add(raceSelect).left();
			VisTextButton removeButton = new VisTextButton("Remove");
			removeButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					sourceData.remove(raceName);
					reload();
				}
			});
			this.add(removeButton).right().row();
		}

		if (sourceData.size() < raceDictionary.getAll().size()) {
			VisTextButton addAnotherButton = new VisTextButton("Add another");
			addAnotherButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					raceDictionary.getAll().stream()
							.filter(race -> !sourceData.contains(race.getName()))
							.findFirst()
							.ifPresent(race -> {
								sourceData.add(race.getName());
								reload();
							});
				}
			});
			this.add(addAnotherButton).left().colspan(2).row();
		}
		this.add(new Separator()).colspan(2).row();

	}
}
