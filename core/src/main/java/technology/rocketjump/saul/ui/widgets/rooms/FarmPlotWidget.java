package technology.rocketjump.saul.ui.widgets.rooms;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.FarmPlotComponent;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.views.RoomEditorItemMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesType.CROP;

public class FarmPlotWidget extends Table {

	private static final int ITEMS_PER_ROW = 8;
	private final Room selectedRoom;
	private final FarmPlotComponent farmPlotComponent;
	private final Skin skin;
	private final TooltipFactory tooltipFactory;
	private final MessageDispatcher messageDispatcher;

	private final List<SeedButton> seedButtons = new ArrayList<>();
	private final EntityRenderer entityRenderer;
	private final I18nTranslator i18nTranslator;

	private Consumer<PlantSpecies> onSeedChange;

	public FarmPlotWidget(Room selectedRoom, FarmPlotComponent farmPlotComponent, Skin skin, TooltipFactory tooltipFactory,
						  MessageDispatcher messageDispatcher, PlantSpeciesDictionary plantSpeciesDictionary,
						  RoomEditorItemMap itemMap, EntityRenderer entityRenderer, I18nTranslator i18nTranslator,
						  SoundAssetDictionary soundAssetDictionary) {
		this.selectedRoom = selectedRoom;
		this.farmPlotComponent = farmPlotComponent;
		this.skin = skin;
		this.tooltipFactory = tooltipFactory;
		this.messageDispatcher = messageDispatcher;
		this.entityRenderer = entityRenderer;
		this.i18nTranslator = i18nTranslator;

		GameContext nullContext = new GameContext();
		for (PlantSpecies plantSpecies : plantSpeciesDictionary.getAll()) {
			if (plantSpecies.getPlantType().equals(CROP) && plantSpecies.getSeed() != null) {
				Entity seedItem = itemMap.getByItemType(plantSpecies.getSeed().getSeedItemType()).clone(messageDispatcher, nullContext);
				((ItemEntityAttributes) seedItem.getPhysicalEntityComponent().getAttributes()).setMaterial(plantSpecies.getSeed().getSeedMaterial());
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, seedItem);
				SeedButton seedButton = new SeedButton(plantSpecies, seedItem, skin, tooltipFactory, messageDispatcher,
						this.entityRenderer, this.i18nTranslator, soundAssetDictionary);
				seedButton.onClick(() -> seedSelected(seedButton.isChecked() ? plantSpecies : null));
				seedButtons.add(seedButton);
			}
		}

		seedButtons.sort(Comparator.comparing(s -> i18nTranslator.getTranslatedString(s.getPlantSpecies().getSeed().getSeedMaterial().getI18nKey()).toString()));

		int index = 0;
		for (SeedButton seedButton : seedButtons) {
			this.add(seedButton);
			index++;
			if (index % ITEMS_PER_ROW == 0) {
				this.row();
			}
		}

		seedSelected(farmPlotComponent.getSelectedCrop());
	}

	private void seedSelected(PlantSpecies selectedPlant) {
		for (SeedButton seedButton : seedButtons) {
			seedButton.setChecked(seedButton.getPlantSpecies().equals(selectedPlant));
		}

		farmPlotComponent.setSelectedCrop(selectedPlant);

		if (onSeedChange != null) {
			onSeedChange.accept(selectedPlant);
		}
	}

	public void setOnSeedChange(Consumer<PlantSpecies> onSeedChange) {
		this.onSeedChange = onSeedChange;
	}
}
