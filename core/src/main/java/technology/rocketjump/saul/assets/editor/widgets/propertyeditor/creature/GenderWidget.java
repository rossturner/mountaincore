package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.model.physical.creature.RaceGenderDescriptor;
import technology.rocketjump.saul.misc.ReflectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.addFloatField;
import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;

public class GenderWidget extends VisTable {

	private final Map<Gender, RaceGenderDescriptor> sourceData;
	private final Collection<EntityAssetType> applicableTypes;
	private final VisTextButton addButton;

	public GenderWidget(Map<Gender, RaceGenderDescriptor> sourceData, Collection<EntityAssetType> applicableTypes) {
		this.sourceData = sourceData;
		this.applicableTypes = applicableTypes;

		addButton = new VisTextButton("Add another");
		addButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gender next = null;
				for (Gender g : Gender.values()) {
					if (!sourceData.containsKey(g)) {
						next = g;
						break;
					}
				}
				if (next != null) {
					sourceData.put(next, new RaceGenderDescriptor());
					reload();
				}
			}
		});

		this.reload();
	}

	private void reload() {
		this.clearChildren();

		for (Map.Entry<Gender, RaceGenderDescriptor> entry : sourceData.entrySet()) {
			VisSelectBox<Gender> genderSelect = new VisSelectBox<>();
			genderSelect.setItems(orderedArray(List.of(Gender.values())));
			genderSelect.setSelected(entry.getKey());
			genderSelect.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					Gender oldSelection = entry.getKey();
					Gender newSelection = genderSelect.getSelected();

					sourceData.put(newSelection, sourceData.get(oldSelection));
					sourceData.remove(oldSelection);
					reload();
				}
			});
			this.add(genderSelect).left();

			try {
				addFloatField("Weighting:", "weighting", entry.getValue(), this);
				this.row();

				this.add(new VisLabel("Hide asset types:")).colspan(3).center().row();

				this.add(new HideAssetTypesWidget(entry.getValue().getHideAssetTypes(), applicableTypes)).colspan(3).left().row();

				VisTextButton removeButton = new VisTextButton("(remove gender)");
				removeButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						sourceData.remove(entry.getKey());
						reload();
					}
				});
				this.add(removeButton).colspan(3).left().row();
			} catch (ReflectionUtils.PropertyReflectionException ignored) {
			}

			this.addSeparator().colspan(3).row();
		}

		this.add(addButton).right().colspan(3).row();
	}

}
