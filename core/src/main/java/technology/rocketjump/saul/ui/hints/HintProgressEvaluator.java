package technology.rocketjump.saul.ui.hints;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.rooms.RoomTypeDictionary;
import technology.rocketjump.saul.rooms.components.FarmPlotComponent;
import technology.rocketjump.saul.rooms.components.StockpileComponent;
import technology.rocketjump.saul.settlement.FurnitureTracker;
import technology.rocketjump.saul.settlement.ItemTracker;
import technology.rocketjump.saul.settlement.SettlerTracker;
import technology.rocketjump.saul.ui.hints.model.HintProgress;
import technology.rocketjump.saul.ui.hints.model.HintProgressDescriptor;
import technology.rocketjump.saul.ui.i18n.I18nString;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWordClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class HintProgressEvaluator implements GameContextAware {

	private final I18nTranslator i18nTranslator;
	private final RoomTypeDictionary roomTypeDictionary;
	private final RoomStore roomStore;
	private final SkillDictionary skillDictionary;
	private final SettlerTracker settlerTracker;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final FurnitureTracker furnitureTracker;
	private final ItemTypeDictionary itemTypeDictionary;
	private final ItemTracker itemTracker;
	private final StockpileGroupDictionary stockpileGroupDictionary;

	private GameContext gameContext;

	@Inject
	public HintProgressEvaluator(I18nTranslator i18nTranslator, RoomTypeDictionary roomTypeDictionary, RoomStore roomStore,
								 SkillDictionary skillDictionary, SettlerTracker settlerTracker,
								 FurnitureTypeDictionary furnitureTypeDictionary, FurnitureTracker furnitureTracker,
								 ItemTypeDictionary itemTypeDictionary, ItemTracker itemTracker,
								 StockpileGroupDictionary stockpileGroupDictionary) {
		this.i18nTranslator = i18nTranslator;
		this.roomTypeDictionary = roomTypeDictionary;
		this.roomStore = roomStore;
		this.skillDictionary = skillDictionary;
		this.settlerTracker = settlerTracker;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.furnitureTracker = furnitureTracker;
		this.itemTypeDictionary = itemTypeDictionary;
		this.itemTracker = itemTracker;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
	}

	public HintProgress evaluate(HintProgressDescriptor descriptor) {
		I18nText targetDescription = I18nText.BLANK;
		int quantity = 0;
		int total = descriptor.getQuantityRequired();

		switch (descriptor.getType()) {
			case ROOMS: {
				RoomType targetType = roomTypeDictionary.getByName(descriptor.getTargetTypeName());
				if (targetType == null) {
					Logger.error("Could not find room by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					quantity = roomStore.getByType(targetType).size();
					targetDescription = i18nTranslator.getTranslatedString(targetType.getI18nKey(), I18nWordClass.PLURAL);
				}
				break;
			}
			case ROOM_TILES: {
				RoomType targetType = roomTypeDictionary.getByName(descriptor.getTargetTypeName());
				if (targetType == null) {
					Logger.error("Could not find room by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					for (Room room : roomStore.getByType(targetType)) {
						quantity += room.getRoomTiles().size();
					}

					Map<String, I18nString> replacements = new HashMap<>();
					replacements.put("roomName", i18nTranslator.getTranslatedString(targetType.getI18nKey(), I18nWordClass.NOUN));
					targetDescription = i18nTranslator.getTranslatedWordWithReplacements("TUTORIAL.PROGRESS_DESCRIPTION.TILES", replacements);
				}
				break;
			}
			case STOCKPILE_TILES: {
				StockpileGroup targetType = stockpileGroupDictionary.getByName(descriptor.getTargetTypeName());
				if (targetType == null) {
					Logger.error("Could not find stockpile group by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					for (Room stockpile : roomStore.getByComponent(StockpileComponent.class)) {
						StockpileComponent stockpileComponent = stockpile.getComponent(StockpileComponent.class);
						if (stockpileComponent.isEnabled(targetType)) {
							quantity += stockpile.getRoomTiles().size();
						}
					}

					Map<String, I18nString> replacements = new HashMap<>();
					replacements.put("roomName", i18nTranslator.getTranslatedString(targetType.getI18nKey(), I18nWordClass.NOUN));
					targetDescription = i18nTranslator.getTranslatedWordWithReplacements("TUTORIAL.PROGRESS_DESCRIPTION.TILES", replacements);
				}
				break;
			}
			case FARM_PLOT_SELECTIONS: {
				List<Room> farmPlots = roomStore.getByComponent(FarmPlotComponent.class);
				quantity = (int) farmPlots.stream()
						.map(plot -> plot.getComponent(FarmPlotComponent.class))
						.filter(component -> component.getSelectedCrop() != null)
						.count();
				targetDescription = i18nTranslator.getTranslatedString("TUTORIAL.PROGRESS_DESCRIPTION.FARM_PLOT_SELECTIONS");
				break;
			}
			case PROFESSIONS_ASSIGNED: {
				Skill requiredType = skillDictionary.getByName(descriptor.getTargetTypeName());
				if (requiredType == null) {
					Logger.error("Could not find profession by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					for (Entity settler : settlerTracker.getLiving()) {
						SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
						if (skillsComponent != null) {
							if (skillsComponent.hasActiveProfession(requiredType)) {
								quantity++;
							}
						}
					}

					Map<String, I18nString> replacements = new HashMap<>();
					replacements.put("professionPlural", i18nTranslator.getTranslatedString(requiredType.getI18nKey(), I18nWordClass.PLURAL));
					targetDescription = i18nTranslator.getTranslatedWordWithReplacements("TUTORIAL.PROGRESS_DESCRIPTION.PROFESSIONS_ASSIGNED", replacements);
				}
				break;
			}
			case FURNITURE_CONSTRUCTED: {
				FurnitureType requiredType = furnitureTypeDictionary.getByName(descriptor.getTargetTypeName());
				if (requiredType == null) {
					Logger.error("Could not find furniture by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					quantity = furnitureTracker.findByFurnitureType(requiredType, false).size();
					targetDescription = i18nTranslator.getTranslatedString(requiredType.getI18nKey(), I18nWordClass.PLURAL);
				}
				break;
			}
			case ITEM_EXISTS: {
				ItemType requiredType = itemTypeDictionary.getByName(descriptor.getTargetTypeName());
				if (requiredType == null) {
					Logger.error("Could not find item by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					for (Entity entity : itemTracker.getItemsByType(requiredType, false)) {
						ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
						quantity += attributes.getQuantity();
					}
					targetDescription = i18nTranslator.getTranslatedString(requiredType.getI18nKey(), I18nWordClass.PLURAL);
				}
				break;
			}
			default:
				Logger.error("Not yet implemented: " + descriptor.getType() + " in " + this.getClass().getSimpleName());
		}


		if (quantity > total) {
			quantity = total;
		}
		return new HintProgress(quantity, total, targetDescription);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
