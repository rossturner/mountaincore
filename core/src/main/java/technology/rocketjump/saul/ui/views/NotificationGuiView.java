package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.combat.CombatTracker;
import technology.rocketjump.saul.entities.behaviour.creature.InvasionCreatureGroup;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.settlement.notifications.Notification;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.GameDialogDictionary;
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;
import technology.rocketjump.saul.ui.widgets.NotificationDialog;

import java.util.ArrayList;

import static technology.rocketjump.saul.audio.MusicJukebox.getCurrentInvasion;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

@Singleton
public class NotificationGuiView implements GuiView, GameContextAware, Telegraph, DisplaysText {

	private static final float TIME_BETWEEN_UPDATES = 4f;
	private final MessageDispatcher messageDispatcher;
	private final IconButtonFactory iconButtonFactory;
	private final GameDialogDictionary gameDialogDictionary;
	private final CombatTracker combatTracker;
	private final TooltipFactory tooltipFactory;
	private final SoundAsset openNotificationSound;
	private Table table;
	private GameContext gameContext;

	private final Button invasionInProgressButton;
	private final Button combatInProgressButton;
	private int combatSelectionCursor = -1;

	private boolean invasionInProgress; // TODO probably want this held more centrally and with messages to say when it changes
	private float timeSinceLastUpdate;

	@Inject
	public NotificationGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							   IconButtonFactory iconButtonFactory, GameDialogDictionary gameDialogDictionary,
							   CombatTracker combatTracker, SoundAssetDictionary soundAssetDictionary,
							   TooltipFactory tooltipFactory) {
		this.iconButtonFactory = iconButtonFactory;
		this.messageDispatcher = messageDispatcher;
		this.gameDialogDictionary = gameDialogDictionary;
		this.combatTracker = combatTracker;
		this.tooltipFactory = tooltipFactory;
		Skin skin = guiSkinRepository.getMainGameSkin();
		this.openNotificationSound = soundAssetDictionary.getByName("NotificationOpen");

		table = new Table(skin);
		table.pad(10);
		table.defaults().pad(16);

		messageDispatcher.addListener(this, MessageType.POST_NOTIFICATION);
		messageDispatcher.addListener(this, MessageType.CREATURE_ENTERED_COMBAT);
		messageDispatcher.addListener(this, MessageType.CREATURE_EXITED_COMBAT);
		messageDispatcher.addListener(this, MessageType.INVASION_ABOUT_TO_BEGIN);

		invasionInProgressButton = new Button(skin.getDrawable("asset_battle_alert"));
		combatInProgressButton = new Button(skin.getDrawable("asset_combat_alert"));
	}

	private Entity getNextEntityInCombat() {
		ArrayList<Entity> entitiesInCombat = new ArrayList<>(combatTracker.getEntitiesInCombat());
		combatSelectionCursor++;
		if (combatSelectionCursor >= entitiesInCombat.size()) {
			combatSelectionCursor = 0;
		}
		if (entitiesInCombat.isEmpty()) {
			return null;
		} else {
			return entitiesInCombat.get(combatSelectionCursor);
		}
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(table).right();
	}

	@Override
	public void rebuildUI() {
		combatInProgressButton.clearListeners();
		combatInProgressButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Entity entity = getNextEntityInCombat();
				if (entity != null) {
					messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, entity.getLocationComponent().getWorldOrParentPosition());
					messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(entity, 0));
				}
			}
		});
		combatInProgressButton.addListener(new ChangeCursorOnHover(combatInProgressButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(combatInProgressButton, "GUI.COMBAT_LABEL", TooltipLocationHint.BELOW);

		invasionInProgressButton.clearListeners();
		invasionInProgressButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				InvasionCreatureGroup invasionGroup = getCurrentInvasion(gameContext);
				if (invasionGroup != null) {
					messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, toVector(invasionGroup.getHomeLocation()));
				}
			}
		});
		invasionInProgressButton.addListener(new ChangeCursorOnHover(invasionInProgressButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(invasionInProgressButton, "GUI.INVASION_LABEL", TooltipLocationHint.BELOW);

		table.clearChildren();

		if (invasionInProgress) {
			table.add(invasionInProgressButton).right().row();
		}
		if (!combatTracker.getEntitiesInCombat().isEmpty()) {
			table.add(combatInProgressButton).right().row();
		}
	}

	@Override
	public void update() {
		if (invasionInProgress) {
			timeSinceLastUpdate += Gdx.graphics.getDeltaTime();
			if (timeSinceLastUpdate > TIME_BETWEEN_UPDATES) {
				InvasionCreatureGroup currentInvasion = getCurrentInvasion(gameContext);
				if (currentInvasion == null || currentInvasion.getMemberIds().isEmpty()) {
					invasionInProgress = false;
					rebuildUI();
				}
			}
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.NOTIFICATION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		invasionInProgress = getCurrentInvasion(gameContext) != null;
		rebuildUI();
	}

	@Override
	public void clearContextRelatedState() {
		table.clearChildren();
		combatSelectionCursor = -1;

		timeSinceLastUpdate = 0;
		invasionInProgress = false;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.CREATURE_ENTERED_COMBAT:
			case MessageType.CREATURE_EXITED_COMBAT: {
				rebuildUI();
				return true;
			}
			case MessageType.INVASION_ABOUT_TO_BEGIN: {
				this.invasionInProgress = true;
				rebuildUI();
				return false;
			}
			case MessageType.POST_NOTIFICATION: {
				Notification notification = (Notification) msg.extraInfo;

				if (gameContext.getSettlementState().suppressedNotificationTypes.contains(notification.getType())) {
					// Player has requested not to receive any more of this type
					return true;
				}

				SoundAsset notificationSound = openNotificationSound;
				if (notification.getType().getOverrideSoundAsset() != null) {
					notificationSound = notification.getType().getOverrideSoundAsset();
				}
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(notificationSound));

				if (!gameContext.getGameClock().isPaused()) {
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
				}
				NotificationDialog notificationDialog = gameDialogDictionary.create(notification);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, notificationDialog);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

}
