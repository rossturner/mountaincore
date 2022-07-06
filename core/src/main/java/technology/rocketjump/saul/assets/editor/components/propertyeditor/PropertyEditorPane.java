package technology.rocketjump.saul.assets.editor.components.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.*;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.*;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.jobs.ProfessionDictionary.NULL_PROFESSION;

public class PropertyEditorPane extends VisTable {

	public static final String NOT_APPLICABLE = "(any)";
	private final VisTable editorTable;
	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final RaceDictionary raceDictionary;
	private final ProfessionDictionary professionDictionary;

	@Inject
	public PropertyEditorPane(EntityAssetTypeDictionary entityAssetTypeDictionary, RaceDictionary raceDictionary, ProfessionDictionary professionDictionary) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.raceDictionary = raceDictionary;
		this.professionDictionary = professionDictionary;
		editorTable = new VisTable();
		VisScrollPane editorScrollPane = new VisScrollPane(editorTable);

		showControlsFor(null);

//		this.setDebug(true);
		this.background("window-bg");
		this.add(new VisLabel("Property Editor")).left().row();
		this.add(editorScrollPane).pad(10).top().row();
		this.add(new VisTable()).expandY();
	}

	public void showControlsFor(Object instance) {
		editorTable.clearChildren();

		if (instance == null) {
			editorTable.add(new VisLabel("Select an item to edit"));
			return;
		}

		switch (instance) {
			case CreatureEntityAsset a -> showEditorControls(a);
			case Race race -> showEditorControls(race);
			case PlantSpecies plantSpecies -> showEditorControls(plantSpecies);
			default ->
					Logger.warn("Not yet implemented: Contrls for " + instance.getClass().getSimpleName() + " in " + getClass().getSimpleName());
		}

	}

	private void showEditorControls(CreatureEntityAsset creatureAsset) {

		VisTextField uniqueNameField = new VisTextField(creatureAsset.getUniqueName());
		uniqueNameField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setUniqueName(uniqueNameField.getText());
			}
		});
		editorTable.add(new VisLabel("Unique name:")).left().row();
		editorTable.add(uniqueNameField).left().fillX().row();

		VisSelectBox<EntityAssetType> assetTypeSelectBox = new VisSelectBox<>();
		assetTypeSelectBox.setItems(orderedArray(entityAssetTypeDictionary.getByEntityType(EntityType.CREATURE)));
		assetTypeSelectBox.setSelected(creatureAsset.getType());
		assetTypeSelectBox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setType(assetTypeSelectBox.getSelected());
			}
		});
		editorTable.add(new VisLabel("Asset type:")).left().row();
		editorTable.add(assetTypeSelectBox).left().row();

		// TODO default race based on selected entity
//		VisSelectBox<Race> raceSelectBox = new VisSelectBox<>();
//		raceSelectBox.setItems(orderedArray(raceDictionary.getAll()));
//		raceSelectBox.setSelected(creatureAsset.getRace());
//		raceSelectBox.addListener(new ChangeListener() {
//			@Override
//			public void changed(ChangeEvent event, Actor actor) {
//				creatureAsset.setRace(raceSelectBox.getSelected());
//			}
//		});
//		editorTable.add(new VisLabel("Race:")).left().row();
//		editorTable.add(raceSelectBox).left().row();

		VisSelectBox<CreatureBodyShape> bodyShapeSelect = new VisSelectBox<>();
		bodyShapeSelect.setItems(new Array<>(CreatureBodyShape.values()));
		if (creatureAsset.getBodyShape() == null) {
			bodyShapeSelect.setSelected(CreatureBodyShape.ANY);
		} else {
			bodyShapeSelect.setSelected(creatureAsset.getBodyShape());
		}
		bodyShapeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setBodyShape(bodyShapeSelect.getSelected());
			}
		});
		editorTable.add(new VisLabel("Body shape:")).left().row();
		editorTable.add(bodyShapeSelect).left().row();

		VisSelectBox<Gender> genderSelect = new VisSelectBox<>();
		genderSelect.setItems(new Array<>(Gender.values()));
		if (creatureAsset.getGender() == null) {
			genderSelect.setSelected(Gender.ANY);
		} else {
			genderSelect.setSelected(creatureAsset.getGender());
		}
		genderSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setGender(genderSelect.getSelected());
			}
		});
		editorTable.add(new VisLabel("Gender:")).left().row();
		editorTable.add(genderSelect).left().row();

		VisSelectBox<Profession> professionSelect = new VisSelectBox<>();
		professionSelect.setItems(orderedArray(professionDictionary.getAll(), NULL_PROFESSION));
		if (creatureAsset.getProfession() == null) {
			professionSelect.setSelected(NULL_PROFESSION);
		} else {
			professionSelect.setSelected(professionDictionary.getByName(creatureAsset.getProfession()));
		}
		professionSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				creatureAsset.setProfession(professionSelect.getSelected().getName());
			}
		});
		editorTable.add(new VisLabel("Profession:")).left().row();
		editorTable.add(professionSelect).left().row();

		VisSelectBox<String> sanitySelect = new VisSelectBox<>();
		sanitySelect.setItems(orderedArray(
				Arrays.stream(Sanity.values()).map(Sanity::name).collect(Collectors.toList()),
				NOT_APPLICABLE));
		sanitySelect.setSelected(creatureAsset.getSanity() == null ? NOT_APPLICABLE : creatureAsset.getSanity().name());
		sanitySelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String selected = sanitySelect.getSelected();
				creatureAsset.setSanity(selected.equals(NOT_APPLICABLE) ? null : Sanity.valueOf(selected));
			}
		});
		editorTable.add(new VisLabel("Sanity:")).left().row();
		editorTable.add(sanitySelect).left().row();

		if (creatureAsset.getConsciousnessList() == null) {
			creatureAsset.setConsciousnessList(new ArrayList<>());
		}
		if (creatureAsset.getConsciousness() != null && !creatureAsset.getConsciousnessList().contains(creatureAsset.getConsciousness())) {
			creatureAsset.getConsciousnessList().add(creatureAsset.getConsciousness());
		}
		VisLabel consciousnessLabel = new VisLabel("Consciousness (click to show)");
		VisTable consciousnessTable = new VisTable();
		for (Consciousness value : Consciousness.values()) {
			VisCheckBox valueCheckbox = new VisCheckBox(value.name());
			valueCheckbox.setChecked(creatureAsset.getConsciousnessList().contains(value));
			valueCheckbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (valueCheckbox.isChecked()) {
						creatureAsset.getConsciousnessList().add(value);
					} else {
						creatureAsset.getConsciousnessList().remove(value);
					}
				}
			});
			consciousnessTable.add(valueCheckbox).left().padLeft(20).row();
		}
		CollapsibleWidget consciousnessCollapsible = new CollapsibleWidget(consciousnessTable);
		consciousnessCollapsible.setCollapsed(creatureAsset.getConsciousnessList().isEmpty());
		consciousnessLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				consciousnessCollapsible.setCollapsed(!consciousnessCollapsible.isCollapsed());
			}
		});
		editorTable.add(consciousnessLabel).left().row();
		editorTable.add(consciousnessCollapsible).left().row();


		VisLabel tagsLabel = new VisLabel("Tags (click to show)");
		TagsComponent tagsComponent = new TagsComponent(creatureAsset.getTags());
		CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsComponent);
		tagsCollapsible.setCollapsed(creatureAsset.getTags().isEmpty());
		tagsLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				tagsCollapsible.setCollapsed(!tagsCollapsible.isCollapsed());
			}
		});
		editorTable.add(tagsLabel).left().expandX().fillX().row();
		editorTable.add(tagsCollapsible).expandX().fillX().left().row();
	}

	private void showEditorControls(Race race) {

	}

	private void showEditorControls(PlantSpecies species) {

	}

	private <T> Array<T> orderedArray(Collection<T> items) {
		return orderedArray(items, null);
	}

	private <T> Array<T> orderedArray(Collection<T> items, T nullItem) {
		Array<T> array = new Array<>();
		items.forEach(array::add);
		array.sort(Comparator.comparing(Object::toString));
		if (nullItem != null) {
			array.insert(0, nullItem);
		}
		return array;
	}
}
