package technology.rocketjump.mountaincore.ui.hints;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.environment.model.GameSpeed;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.RoomStore;
import technology.rocketjump.mountaincore.rooms.RoomType;
import technology.rocketjump.mountaincore.rooms.RoomTypeDictionary;
import technology.rocketjump.mountaincore.rooms.components.FarmPlotComponent;
import technology.rocketjump.mountaincore.rooms.components.StockpileRoomComponent;
import technology.rocketjump.mountaincore.screens.ManagementScreenName;
import technology.rocketjump.mountaincore.settlement.SettlementFurnitureTracker;
import technology.rocketjump.mountaincore.settlement.SettlementItemTracker;
import technology.rocketjump.mountaincore.settlement.SettlerTracker;
import technology.rocketjump.mountaincore.ui.GameViewMode;
import technology.rocketjump.mountaincore.ui.hints.model.HintProgress;
import technology.rocketjump.mountaincore.ui.hints.model.HintProgressDescriptor;
import technology.rocketjump.mountaincore.ui.i18n.I18nString;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.I18nWordClass;

import java.util.*;

@Singleton
public class HintProgressEvaluator implements GameContextAware, Telegraph {

	private final I18nTranslator i18nTranslator;
	private final RoomTypeDictionary roomTypeDictionary;
	private final RoomStore roomStore;
	private final SkillDictionary skillDictionary;
	private final SettlerTracker settlerTracker;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final SettlementFurnitureTracker settlementFurnitureTracker;
	private final ItemTypeDictionary itemTypeDictionary;
	private final SettlementItemTracker settlementItemTracker;
	private final StockpileGroupDictionary stockpileGroupDictionary;

	private GameContext gameContext;
	private Set<HintProgressDescriptor.ProgressDescriptorTargetType> completedTargets = new HashSet<>();

	@Inject
	public HintProgressEvaluator(I18nTranslator i18nTranslator, RoomTypeDictionary roomTypeDictionary, RoomStore roomStore,
								 SkillDictionary skillDictionary, SettlerTracker settlerTracker,
								 FurnitureTypeDictionary furnitureTypeDictionary, SettlementFurnitureTracker settlementFurnitureTracker,
								 ItemTypeDictionary itemTypeDictionary, SettlementItemTracker settlementItemTracker,
								 StockpileGroupDictionary stockpileGroupDictionary, MessageDispatcher messageDispatcher) {
		this.i18nTranslator = i18nTranslator;
		this.roomTypeDictionary = roomTypeDictionary;
		this.roomStore = roomStore;
		this.skillDictionary = skillDictionary;
		this.settlerTracker = settlerTracker;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.settlementFurnitureTracker = settlementFurnitureTracker;
		this.itemTypeDictionary = itemTypeDictionary;
		this.settlementItemTracker = settlementItemTracker;
		this.stockpileGroupDictionary = stockpileGroupDictionary;

		messageDispatcher.addListener(this, MessageType.TUTORIAL_TRACKING_CAMERA_PANNED);
		messageDispatcher.addListener(this, MessageType.TUTORIAL_TRACKING_CAMERA_ZOOMED);
		messageDispatcher.addListener(this, MessageType.TUTORIAL_TRACKING_MINIMAP_CLICKED);
		messageDispatcher.addListener(this, MessageType.SET_GAME_SPEED);
		messageDispatcher.addListener(this, MessageType.GUI_SWITCH_VIEW_MODE);
		messageDispatcher.addListener(this, MessageType.SWITCH_SCREEN);
		messageDispatcher.addListener(this, MessageType.REQUEST_SAVE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.TUTORIAL_TRACKING_CAMERA_PANNED -> {
				completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.CAMERA_MOVED);
			}
			case MessageType.TUTORIAL_TRACKING_CAMERA_ZOOMED -> {
				completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.CAMERA_ZOOMED);
			}
			case MessageType.TUTORIAL_TRACKING_MINIMAP_CLICKED -> {
				completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.MINIMAP_CLICKED);
			}
			case MessageType.SET_GAME_SPEED -> {
				GameSpeed speed = (GameSpeed) msg.extraInfo;
				switch (speed) {
					case PAUSED -> {
						completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.GAME_PAUSED);
					}
					case NORMAL -> {
						completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.NORMAL_SPEED_SELECTED);
					}
					default -> {
						completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.FAST_SPEED_SELECTED);
					}
				}
			}
			case MessageType.GUI_SWITCH_VIEW_MODE -> {
				GameViewMode viewMode = (GameViewMode) msg.extraInfo;
				if (viewMode.equals(GameViewMode.DEFAULT)) {
					completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.DEFAULT_VIEW_MODE);
				} else {
					completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.OTHER_VIEW_MODE);
				}
			}
			case MessageType.SWITCH_SCREEN -> {
				String screenName = (String) msg.extraInfo;
				if (screenName.equals(ManagementScreenName.RESOURCES.name())) {
					completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.RESOURCE_MANAGEMENT);
				} else if (screenName.equals(ManagementScreenName.SETTLERS.name())) {
					completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.SETTLER_MANAGEMENT);
				}
			}
			case MessageType.REQUEST_SAVE -> {
				completedTargets.add(HintProgressDescriptor.ProgressDescriptorTargetType.GAME_SAVED);
			}
			default -> {
				Logger.error("Unexpected message type " + msg.message + " received by " + this.getClass().getSimpleName() + ", " + msg.toString());
			}
		}
		return true;
	}

	public HintProgress evaluate(HintProgressDescriptor descriptor) {
		I18nText targetDescription = I18nText.BLANK;
		int quantity = 0;
		int total = Math.max(descriptor.getQuantityRequired(), 1);

		switch (descriptor.getType()) {
			case ROOMS -> {
				RoomType targetType = roomTypeDictionary.getByName(descriptor.getTargetTypeName());
				if (targetType == null) {
					Logger.error("Could not find room by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					quantity = roomStore.getByType(targetType).size();
					targetDescription = i18nTranslator.getTranslatedString(targetType.getI18nKey(), I18nWordClass.PLURAL);
				}
			}
			case ROOM_TILES -> {
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
			}
			case STOCKPILE_TILES -> {
				StockpileGroup targetType = stockpileGroupDictionary.getByName(descriptor.getTargetTypeName());
				if (targetType == null) {
					Logger.error("Could not find stockpile group by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					for (Room stockpile : roomStore.getByComponent(StockpileRoomComponent.class)) {
						StockpileRoomComponent stockpileRoomComponent = stockpile.getComponent(StockpileRoomComponent.class);
						if (stockpileRoomComponent.getStockpileSettings().isEnabled(targetType)) {
							quantity += stockpile.getRoomTiles().size();
						}
					}

					Map<String, I18nString> replacements = new HashMap<>();
					replacements.put("roomName", i18nTranslator.getTranslatedString(targetType.getI18nKey(), I18nWordClass.NOUN));
					targetDescription = i18nTranslator.getTranslatedWordWithReplacements("TUTORIAL.PROGRESS_DESCRIPTION.TILES", replacements);
				}
			}
			case FARM_PLOT_SELECTIONS -> {
				List<Room> farmPlots = roomStore.getByComponent(FarmPlotComponent.class);
				quantity = (int) farmPlots.stream()
						.map(plot -> plot.getComponent(FarmPlotComponent.class))
						.filter(component -> component.getSelectedCrop() != null)
						.count();
				targetDescription = i18nTranslator.getTranslatedString("TUTORIAL.PROGRESS_DESCRIPTION.FARM_PLOT_SELECTIONS");
				break;
			}
			case PROFESSIONS_ASSIGNED -> {
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
			}
			case FURNITURE_CONSTRUCTED -> {
				FurnitureType requiredType = furnitureTypeDictionary.getByName(descriptor.getTargetTypeName());
				if (requiredType == null) {
					Logger.error("Could not find furniture by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					quantity = settlementFurnitureTracker.findByFurnitureType(requiredType, false).size();
					targetDescription = i18nTranslator.getTranslatedString(requiredType.getI18nKey(), I18nWordClass.PLURAL);
				}
			}
			case ITEM_EXISTS -> {
				ItemType requiredType = itemTypeDictionary.getByName(descriptor.getTargetTypeName());
				if (requiredType == null) {
					Logger.error("Could not find item by type " + descriptor.getTargetTypeName() + " when evaluating " + descriptor.toString());
				} else {
					for (Entity entity : settlementItemTracker.getItemsByType(requiredType, false)) {
						ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
						quantity += attributes.getQuantity();
					}
					targetDescription = i18nTranslator.getTranslatedString(requiredType.getI18nKey(), I18nWordClass.PLURAL);
				}
			}
			default -> {
				quantity = completedTargets.contains(descriptor.getType()) ? 1 : 0;
			}
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
		completedTargets.clear();
	}
}
