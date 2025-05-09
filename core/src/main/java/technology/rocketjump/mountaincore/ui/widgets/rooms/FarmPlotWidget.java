package technology.rocketjump.mountaincore.ui.widgets.rooms;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.rendering.entities.EntityRenderer;
import technology.rocketjump.mountaincore.rooms.components.FarmPlotComponent;
import technology.rocketjump.mountaincore.settlement.SettlementItemTracker;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.ManagementSkin;
import technology.rocketjump.mountaincore.ui.views.RoomEditorItemMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class FarmPlotWidget extends Table {

	private static final int ITEMS_PER_ROW = 8;
	private final FarmPlotComponent farmPlotComponent;

	private final List<SeedButton> seedButtons = new ArrayList<>();

	private Consumer<PlantSpecies> onSeedChange;

	public FarmPlotWidget(FarmPlotComponent farmPlotComponent, Skin skin, TooltipFactory tooltipFactory,
						  MessageDispatcher messageDispatcher, PlantSpeciesDictionary plantSpeciesDictionary,
						  RoomEditorItemMap itemMap, EntityRenderer entityRenderer, I18nTranslator i18nTranslator,
						  SoundAssetDictionary soundAssetDictionary, ManagementSkin managementSkin) {
		this.farmPlotComponent = farmPlotComponent;

		GameContext nullContext = new GameContext();
		for (PlantSpecies plantSpecies : plantSpeciesDictionary.getAll()) {
			if (plantSpecies.getPlantType().equals(PlantSpeciesType.CROP) && plantSpecies.getSeed() != null) {
				Entity seedItem = itemMap.getByItemType(plantSpecies.getSeed().getSeedItemType()).clone(messageDispatcher, nullContext);
				((ItemEntityAttributes) seedItem.getPhysicalEntityComponent().getAttributes()).setMaterial(plantSpecies.getSeed().getSeedMaterial());
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, seedItem);
				SeedButton seedButton = new SeedButton(plantSpecies, seedItem, skin, tooltipFactory, messageDispatcher,
						entityRenderer, i18nTranslator, soundAssetDictionary, managementSkin);
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

	public void updateSeedQuantities(SettlementItemTracker settlementItemTracker) {
		for (SeedButton seedButton : seedButtons) {
			seedButton.updateQuantityLabel(settlementItemTracker);
		}
	}
}
