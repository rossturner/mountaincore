package technology.rocketjump.saul.production;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;

@Singleton
public class StockpileComponentUpdater implements GameContextAware {

	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final RaceDictionary raceDictionary;
	private GameContext gameContext;

	@Inject
	public StockpileComponentUpdater(ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary gameMaterialDictionary, RaceDictionary raceDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.raceDictionary = raceDictionary;
	}

	public void toggleGroup(StockpileSettings stockpileSettings, StockpileGroup group, boolean enabled, boolean recurseToChildren) {
		stockpileSettings.toggle(group, enabled);
//		stockpileComponent.updateColor(); //todo

		if (recurseToChildren) {
			for (ItemType itemType : itemTypeDictionary.getByStockpileGroup(group)) {
				toggleItem(stockpileSettings, itemType, enabled, false, true);
			}
			if (group.isIncludesCreatureCorpses()) {
				toggleCorpseGroup(stockpileSettings, enabled, group,false, true);
			}
		}
	}

	public void toggleItem(StockpileSettings stockpileSettings, ItemType itemType, boolean enabled, boolean recurseToParent, boolean recurseToChildren) {
		stockpileSettings.toggle(itemType, enabled);

		if (recurseToChildren) {
			for (GameMaterial gameMaterial : gameMaterialDictionary.getByType(itemType.getPrimaryMaterialType())) {
				stockpileSettings.toggle(itemType, gameMaterial, enabled);
				toggleMaterial(stockpileSettings, itemType, gameMaterial, enabled, false);
			}
		}

		if (recurseToParent) {
			boolean allGroupChildrenDisabled = allGroupChildrenDisabled(itemType.getStockpileGroup(), stockpileSettings);

			if (enabled) {
				// when enabled, always enable parent(s)
				toggleGroup(stockpileSettings, itemType.getStockpileGroup(), true, false);
			}
			if (allGroupChildrenDisabled) {
				toggleGroup(stockpileSettings, itemType.getStockpileGroup(), false, false);
			}
		}
	}

	public void toggleCorpseGroup(StockpileSettings stockpileSettings, boolean enabled, StockpileGroup parentGroup, boolean recurseToParent, boolean recurseToChildren) {
		stockpileSettings.setAcceptingCorpses(enabled);

		if (recurseToChildren) {
			for (Race race : raceDictionary.getAll()) {
				if (race.equals(gameContext.getSettlementState().getSettlerRace())) {
					continue;
				}
				stockpileSettings.toggleCorpse(race, enabled);
				toggleRaceCorpse(stockpileSettings, race, parentGroup, enabled, false);
			}
		}

		if (recurseToParent) {
			boolean allGroupChildrenDisabled = allGroupChildrenDisabled(parentGroup, stockpileSettings);

			if (enabled) {
				// when enabled, always enable parent(s)
				toggleGroup(stockpileSettings, parentGroup, true, false);
			}
			if (allGroupChildrenDisabled) {
				toggleGroup(stockpileSettings, parentGroup, false, false);
			}
		}
	}

	private boolean allGroupChildrenDisabled(StockpileGroup group, StockpileSettings stockpileSettings) {

		boolean allSiblingsDisabled = true;
		for (ItemType siblingItem : itemTypeDictionary.getByStockpileGroup(group)) {
			if (stockpileSettings.isEnabled(siblingItem)) {
				allSiblingsDisabled = false;
				break;
			}
		}

		if (group.isIncludesCreatureCorpses() && stockpileSettings.isAcceptingCorpses()) {
			allSiblingsDisabled = false;
		}
		return allSiblingsDisabled;
	}

	public void toggleRaceCorpse(StockpileSettings stockpileSettings, Race race, StockpileGroup parentGroup, boolean enabled, boolean recurseToParent) {
		stockpileSettings.toggleCorpse(race, enabled);

		if (recurseToParent) {
			boolean allSiblingsDisabled = true;

			for (Race sibling : raceDictionary.getAll()) {
				if (race.equals(gameContext.getSettlementState().getSettlerRace())) {
					continue;
				}
				if (stockpileSettings.isEnabled(sibling)) {
					allSiblingsDisabled = false;
					break;
				}
			}

			if (enabled) {
				// when enabled, always enable parents
				toggleCorpseGroup(stockpileSettings, true, parentGroup, true, false);
				toggleGroup(stockpileSettings, parentGroup, true, false);
			}
			if (allSiblingsDisabled) {
				toggleCorpseGroup(stockpileSettings, false, parentGroup, true, false); // recurseToParent to toggle group is necessary
			}
		}
	}

	public void toggleMaterial(StockpileSettings stockpileSettings, ItemType itemType, GameMaterial gameMaterial, boolean enabled, boolean recurseToParent) {
		stockpileSettings.toggle(itemType, gameMaterial, enabled);

		if (recurseToParent) {
			boolean allSiblingsDisabled = true;

			for (GameMaterial material : gameMaterialDictionary.getByType(itemType.getPrimaryMaterialType())) {
				if (stockpileSettings.isEnabled(material, itemType)) {
					allSiblingsDisabled = false;
					break;
				}
			}

			if (enabled) {
				// when enabled, always enable parents
				toggleItem(stockpileSettings, itemType, true, true, false);
				toggleGroup(stockpileSettings, itemType.getStockpileGroup(), true, false);
			}
			if (allSiblingsDisabled) {
				toggleItem(stockpileSettings, itemType, false, true, false); // recurseToParent to toggle group is necessary
			}
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
