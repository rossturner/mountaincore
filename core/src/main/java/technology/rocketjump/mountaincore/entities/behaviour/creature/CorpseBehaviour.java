package technology.rocketjump.mountaincore.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.creature.HistoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SteeringComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Gender;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.utils.ColorMixer;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.components.StockpileRoomComponent;
import technology.rocketjump.mountaincore.ui.i18n.I18nString;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorpseBehaviour implements BehaviourComponent, SelectableDescription {

	private SteeringComponent steeringComponent = new SteeringComponent(); // Is this needed?
	private double lastUpdateGameTime;
	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private Color originalSkinColor;
	private Color FULLY_DECAYED_COLOR;
	private double HOURS_TO_FULLY_DECAY;
	private double decayedAmount;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
		HOURS_TO_FULLY_DECAY = gameContext.getConstantsRepo().getWorldConstants().getCorpseDecayHours();
		FULLY_DECAYED_COLOR = gameContext.getConstantsRepo().getWorldConstants().getCorpseDecayColorInstance();
		steeringComponent.init(parentEntity, gameContext.getAreaMap(), messageDispatcher);
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		CorpseBehaviour corpseBehaviour = new CorpseBehaviour();
		corpseBehaviour.originalSkinColor = this.originalSkinColor.cpy();
		return corpseBehaviour;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		ItemAllocationComponent itemAllocationComponent = parentEntity.getComponent(ItemAllocationComponent.class);
		if (itemAllocationComponent == null) {
			itemAllocationComponent = new ItemAllocationComponent();
			itemAllocationComponent.init(parentEntity, messageDispatcher, gameContext);
			parentEntity.addComponent(itemAllocationComponent);
		}
		if (parentEntity.getLocationComponent().getWorldPosition() != null && itemAllocationComponent.getNumUnallocated() > 0) {
			// Is unallocated
			MapTile tile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
			if (tile != null) {
				boolean inStockpile = false;
				if (tile.getRoomTile() != null) {
					Room room = tile.getRoomTile().getRoom();
					StockpileRoomComponent stockpileRoomComponent = room.getComponent(StockpileRoomComponent.class);
					if (stockpileRoomComponent != null && stockpileRoomComponent.getStockpileSettings().canHold(parentEntity)) {
						inStockpile = true;
					}
				}

				if (!inStockpile) {
					// Not in a stockpile and some unallocated, so see if we can be hauled to a stockpile
					messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(parentEntity, parentEntity, false, null, null));
				}
			}
		}

		double elapsedTime = gameContext.getGameClock().getCurrentGameTime() - lastUpdateGameTime;
		decayedAmount += elapsedTime;
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();

		if (decayedAmount < HOURS_TO_FULLY_DECAY) {
			Color newSkinColor = ColorMixer.interpolate(0, (float) HOURS_TO_FULLY_DECAY, (float) decayedAmount, originalSkinColor, FULLY_DECAYED_COLOR);
			attributes.setSkinColor(newSkinColor);
		}

		if (decayedAmount >= HOURS_TO_FULLY_DECAY && !attributes.getGender().equals(Gender.NONE)) {
			// Switch to fully decayed
			setToFullyDecayed(attributes);
		}

		if (parentEntity.getPhysicalEntityComponent().getBaseAsset() == null) {
			// No asset for skeleton or whatever, so just destroy
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, parentEntity);
		}

		if (decayedAmount >= 2 * HOURS_TO_FULLY_DECAY && parentEntity.getLocationComponent().getContainerEntity() == null) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, parentEntity);
		}

		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	public void setToFullyDecayed(CreatureEntityAttributes attributes) {
		attributes.setGender(Gender.NONE);
		SkillsComponent skillsComponent = parentEntity.getComponent(SkillsComponent.class);
		if (skillsComponent != null) {
			skillsComponent.clear();
			skillsComponent.activateProfession(SkillDictionary.NULL_PROFESSION);
		}
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
	}

	public void setOriginalSkinColor(Color originalSkinColor) {
		if (originalSkinColor == null) {
			originalSkinColor = Color.WHITE;
		}
		this.originalSkinColor = originalSkinColor;
	}

	@Override
	public void update(float deltaTime) {
		// Do nothing
	}

	@Override
	public void updateWhenPaused() {

	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return steeringComponent;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {
		HistoryComponent historyComponent = parentEntity.getComponent(HistoryComponent.class);
		if (historyComponent != null && historyComponent.getDeathReason() != null) {
			DeathReason reason = historyComponent.getDeathReason();

			Map<String, I18nString> replacements = new HashMap<>();
			I18nText deathDescriptionString;
			if (reason.equals(DeathReason.KILLED_BY_ENTITY)) {

				if (historyComponent.getKilledBy() == null) {
					replacements.put("killer", i18nTranslator.getDictionary().getWord("DEATH_REASON.MURDERED.UNKNOWN_KILLER"));
				} else {
					replacements.put("killer", i18nTranslator.getDescription(historyComponent.getKilledBy()));
				}
				deathDescriptionString = i18nTranslator.getTranslatedWordWithReplacements("NOTIFICATION.DEATH.KILLED_BY_DESCRIPTION", replacements);
			} else {
				replacements.put("reason", i18nTranslator.getDictionary().getWord(reason.getI18nKey()));
				deathDescriptionString = i18nTranslator.getTranslatedWordWithReplacements("NOTIFICATION.DEATH.SHORT_DESCRIPTION", replacements);
			}
			return List.of(deathDescriptionString);
		} else {
			return List.of(i18nTranslator.getTranslatedString("CREATURE.STATUS.DEAD"));
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("originalSkin", HexColors.toHexString(originalSkinColor));
		asJson.put("decayed", decayedAmount);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		originalSkinColor = HexColors.get(asJson.getString("originalSkin"));
		decayedAmount = asJson.getDoubleValue("decayed");
	}
}
