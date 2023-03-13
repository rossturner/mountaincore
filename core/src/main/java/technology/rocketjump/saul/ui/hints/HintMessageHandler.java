package technology.rocketjump.saul.ui.hints;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.Updatable;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.GuiArea;
import technology.rocketjump.saul.ui.hints.model.Hint;
import technology.rocketjump.saul.ui.hints.model.HintAction;
import technology.rocketjump.saul.ui.hints.model.HintOnShowEvent;
import technology.rocketjump.saul.ui.hints.model.HintTrigger;
import technology.rocketjump.saul.ui.views.GuiViewName;
import technology.rocketjump.saul.ui.views.HintGuiView;

import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.ENABLE_TUTORIAL;
import static technology.rocketjump.saul.screens.ScreenManager.chooseSpawnLocation;
import static technology.rocketjump.saul.ui.hints.model.HintTrigger.HintTriggerType.*;

@Singleton
public class HintMessageHandler implements Telegraph, Updatable {

	private final MessageDispatcher messageDispatcher;
	private final HintDictionary hintDictionary;
	private final HintGuiView hintGuiView;
	private final UserPreferences userPreferences;
	private final ItemTypeDictionary itemTypeDictionary;
	private final SettlementItemTracker settlementItemTracker;
	private GameContext gameContext;
	private float timeSinceLastUpdate;

	@Inject
	public HintMessageHandler(MessageDispatcher messageDispatcher, HintDictionary hintDictionary,
							  HintGuiView hintGuiView, UserPreferences userPreferences, ItemTypeDictionary itemTypeDictionary, SettlementItemTracker settlementItemTracker) {
		this.messageDispatcher = messageDispatcher;
		this.hintDictionary = hintDictionary;
		this.hintGuiView = hintGuiView;
		this.userPreferences = userPreferences;
		this.itemTypeDictionary = itemTypeDictionary;
		this.settlementItemTracker = settlementItemTracker;

		messageDispatcher.addListener(this, MessageType.START_NEW_GAME);
		messageDispatcher.addListener(this, MessageType.GUI_SWITCH_VIEW);
		messageDispatcher.addListener(this, MessageType.HINT_ACTION_TRIGGERED);
		messageDispatcher.addListener(this, MessageType.ENTITY_CREATED);
		messageDispatcher.addListener(this, MessageType.BEGIN_SPAWN_SETTLEMENT);
		messageDispatcher.addListener(this, MessageType.SETTLEMENT_SPAWNED);
	}


	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > 1.141f) {
			timeSinceLastUpdate = 0f;

			if (!hintGuiView.getDisplayedHints().isEmpty()) {
				return;
			}

			if (gameContext != null) {
				boolean newHintDisplayed = false;

				// Check item amount hints
				for (Hint hint : hintDictionary.getByTriggerType(HintTrigger.HintTriggerType.ITEM_AMOUNT)) {
					for (HintTrigger trigger : hint.getTriggers()) {
						ItemType itemType = itemTypeDictionary.getByName(trigger.getRelatedTypeName());
						if (itemType == null) {
							Logger.error("Unrecognised item type " + trigger.getRelatedTypeName() + " for hint " + hint.getHintId());
						} else {
							Integer totalQuantity = settlementItemTracker.getItemsByType(itemType, false).stream()
									.map(entity -> ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getQuantity())
									.reduce(0, Integer::sum);
							if (trigger.getQuantity() == totalQuantity && canBeShown(hint)) {
								show(hint);
								newHintDisplayed = true;
								break;
							}
						}
					}

					if (newHintDisplayed) {
						break;
					}
				}
			}
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.START_NEW_GAME: {
				if (!chooseSpawnLocation(userPreferences)) {
					return false;
				}
				for (Hint hint : hintDictionary.getByTriggerType(ON_GAME_START)) {
					if (canBeShown(hint)) {
						show(hint);
					}
				}
				return false; // Not main handler
			}
			case MessageType.BEGIN_SPAWN_SETTLEMENT: {
				hintGuiView.dismissAll();
				return false; // Not main handler
			}
			case MessageType.SETTLEMENT_SPAWNED: {
				if ((Boolean.parseBoolean(userPreferences.getPreference(ENABLE_TUTORIAL, "true")))) {
					for (Hint hint : hintDictionary.getByTriggerType(ON_START_WITH_TUTORIAL)) {
						if (canBeShown(hint)) {
							show(hint);
						}
					}
				}
				return false; // Not main handler
			}
			case MessageType.GUI_SWITCH_VIEW: {
				GuiViewName viewName = (GuiViewName) msg.extraInfo;

				for (Hint hint : hintDictionary.getByTriggerType(GUI_SWITCH_VIEW)) {
					for (HintTrigger trigger : hint.getTriggers()) {
						if (trigger.getTriggerType().equals(GUI_SWITCH_VIEW) && trigger.getRelatedTypeName().equals(viewName.name()) && canBeShown(hint)) {
							show(hint);
							return false;
						}
					}
				}

				return false; // not primary handler
			}
			case MessageType.ENTITY_CREATED: {
				Entity createdEntity = (Entity) msg.extraInfo;
				if (createdEntity.getType().equals(EntityType.ONGOING_EFFECT)) {
					OngoingEffectType type = ((OngoingEffectAttributes) createdEntity.getPhysicalEntityComponent().getAttributes()).getType();
					for (Hint hint : hintDictionary.getByTriggerType(ONGOING_EFFECT_TRIGGERED)) {
						for (HintTrigger trigger : hint.getTriggers()) {
							if (trigger.getTriggerType().equals(ONGOING_EFFECT_TRIGGERED) && trigger.getRelatedTypeName().equals(type.getName()) && canBeShown(hint)) {
								show(hint);
								return false;
							}
						}
					}
				}
				return false; // not primary handler
			}
			case MessageType.HINT_ACTION_TRIGGERED: {
				HintAction action = (HintAction) msg.extraInfo;

				// In all cases dismiss all existing hints
				hintGuiView.dismissAll();

				switch (action.getType()) {
					case SHOW_OTHER_HINT:
						Hint otherHint = hintDictionary.getById(action.getRelatedHintId());
						if (otherHint != null) {
							if (canBeShown(otherHint)) {
								show(otherHint);
							}
						} else {
							Logger.error("Could not find hint with ID " + action.getRelatedHintId());
						}
						break;
					case DISABLE_TUTORIAL:
						userPreferences.setPreference(ENABLE_TUTORIAL, "false");
						messageDispatcher.dispatchMessage(MessageType.PREFERENCE_CHANGED, ENABLE_TUTORIAL);
						messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_AREA, GuiArea.MILITARY_MANAGEMENT_BUTTON);
						break;
					case DISMISS:
						// Do nothing, but don't hit default handler
						break;
					default:
						Logger.error("Unrecognised " + HintAction.class.getSimpleName() + " in " + this.getClass().getSimpleName() + ": " + action.getType());
				}

				return true;
			}
			default:
				Logger.error("Unexpected message type " + msg.message + " received by " + this.getClass().getSimpleName() + ", " + msg.toString());
				return false;
		}
	}

	private boolean canBeShown(Hint hint) {
		if (!hintGuiView.getDisplayedHints().isEmpty()) {
			return false;
		}

		if (gameContext != null && gameContext.getSettlementState().getPreviousHints().containsKey(hint.getHintId())) {
			return false;
		}

		return true;
	}

	private void show(Hint hint) {
		hintGuiView.add(hint);
		if (hint.getOnShow() != null) {
			for (HintOnShowEvent hintOnShowEvent : hint.getOnShow()) {
				switch (hintOnShowEvent.getType()) {
					case HIDE_ALL_GUI_AREAS -> {
						for (GuiArea guiArea : GuiArea.values()) {
							messageDispatcher.dispatchMessage(MessageType.GUI_HIDE_AREA, guiArea);
						}
					}
					case SHOW_GUI_AREA -> {
						GuiArea guiArea = EnumUtils.getEnum(GuiArea.class, hintOnShowEvent.getValue());
						if (guiArea == null) {
							Logger.error("Could not find " + GuiArea.class.getSimpleName() + " with name " + hintOnShowEvent.getValue());
						} else {
							messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_AREA, guiArea);
						}
					}
					default -> Logger.error("Not yet implemented: " + hintOnShowEvent.getType());
				}
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

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
