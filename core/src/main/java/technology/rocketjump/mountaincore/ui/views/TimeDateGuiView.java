package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.gamecontext.GameState;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.screens.ManagementScreenName;
import technology.rocketjump.mountaincore.ui.GameInteractionStateContainer;
import technology.rocketjump.mountaincore.ui.GameViewMode;
import technology.rocketjump.mountaincore.ui.GuiArea;
import technology.rocketjump.mountaincore.ui.Selectable;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;
import technology.rocketjump.mountaincore.ui.widgets.maingame.TimeDateWidget;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static technology.rocketjump.mountaincore.screens.ManagementScreenName.RESOURCES;
import static technology.rocketjump.mountaincore.screens.ManagementScreenName.SETTLERS;

@Singleton
public class TimeDateGuiView implements GuiView, GameContextAware, Telegraph, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final Skin skin;
	private final Table layoutTable;
	private final Table managementScreenButtonTable;
	private final Table viewModeButtons;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final ButtonFactory buttonFactory;
	private final SoundAssetDictionary soundAssetDictionary;
	private GameContext gameContext;

	private final TimeDateWidget timeDateWidget;
	private Set<GuiArea> hiddenGuiAreas = new HashSet<>();

	@Inject
	public TimeDateGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						   TimeDateWidget timeDateWidget, TooltipFactory tooltipFactory,
						   GameInteractionStateContainer gameInteractionStateContainer,
						   ButtonFactory buttonFactory, SoundAssetDictionary soundAssetDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.skin = guiSkinRepository.getMainGameSkin();

		this.timeDateWidget = timeDateWidget;
		this.buttonFactory = buttonFactory;
		this.soundAssetDictionary = soundAssetDictionary;

		managementScreenButtonTable = new Table();
		managementScreenButtonTable.padTop(38);
		managementScreenButtonTable.padRight(52);
		managementScreenButtonTable.defaults().padLeft(22);

		viewModeButtons = new Table();
		viewModeButtons.defaults().padLeft(20);
		viewModeButtons.setTouchable(Touchable.enabled);

		layoutTable = new Table();
		reset(null);

		messageDispatcher.addListener(this, MessageType.SETTLEMENT_SPAWNED);
		messageDispatcher.addListener(this, MessageType.GUI_VIEW_MODE_CHANGED);
	}

	public void reset(GameContext gameContext) {
		rebuildUI();

		layoutTable.clearChildren();
		if (gameContext == null || !gameContext.getSettlementState().getGameState().equals(GameState.SELECT_SPAWN_LOCATION)) {
			if (anyManagementButtonsShown()) {
				layoutTable.add(managementScreenButtonTable).right().top();
			}
			if (!hiddenGuiAreas.contains(GuiArea.TIME_AND_DATE)) {
				layoutTable.add(timeDateWidget).top().right().padTop(6).row();
			}
			if (!hiddenGuiAreas.contains(GuiArea.VIEW_MODES)) {
				if (anyManagementButtonsShown()) {
					layoutTable.add(new Container<>()); // pad out this cell
				}
				layoutTable.add(viewModeButtons).padLeft(40).center().row();
			}
		}
	}

	boolean anyManagementButtonsShown() {
		return (!hiddenGuiAreas.contains(GuiArea.RESOURCE_MANAGEMENT_BUTTON)) ||
				(!hiddenGuiAreas.contains(GuiArea.MILITARY_MANAGEMENT_BUTTON)) ||
				(!hiddenGuiAreas.contains(GuiArea.SETTLER_MANAGEMENT_BUTTON));
	}

	@Override
	public void rebuildUI() {
		managementScreenButtonTable.clearChildren();

		if (!hiddenGuiAreas.contains(GuiArea.MILITARY_MANAGEMENT_BUTTON)) {
			Button militaryButton = buttonFactory.buildDrawableButton("btn_top_military", "GUI.SETTLER_MANAGEMENT.PROFESSION.MILITARY", TooltipLocationHint.BELOW, () -> {
				Optional<Squad> optionalSquad = gameContext.getSquads().values().stream().findAny();
				optionalSquad.ifPresentOrElse(squad -> messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(squad)),
						() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.SQUAD_SELECTED));
			});
			managementScreenButtonTable.add(militaryButton).size(157f, 170f);
		}

		for (ManagementScreenName managementScreen : ManagementScreenName.managementScreensOrderedForUI) {
			if (managementScreen.equals(SETTLERS) && hiddenGuiAreas.contains(GuiArea.SETTLER_MANAGEMENT_BUTTON)) {
				continue;
			} else if (managementScreen.equals(RESOURCES) && hiddenGuiAreas.contains(GuiArea.RESOURCE_MANAGEMENT_BUTTON)) {
				continue;
			}
			Button screenButton = buttonFactory.buildDrawableButton(managementScreen.buttonStyleName, managementScreen.titleI18nKey, TooltipLocationHint.BELOW, () -> {
				messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, managementScreen.name());
			});
			managementScreenButtonTable.add(screenButton).size(157f,170f);
		}

		viewModeButtons.clearChildren();
		ButtonGroup<Button> viewModeGroup = new ButtonGroup<>();
		viewModeGroup.setMaxCheckCount(1);
		for (GameViewMode viewMode : GameViewMode.values()) {
			Button viewModeButton = new Button(skin.get(viewMode.getButtonStyleName(), Button.ButtonStyle.class));
			if (viewMode.equals(gameInteractionStateContainer.getGameViewMode())) {
				viewModeButton.setChecked(true);
			}

			viewModeButton.addListener(new ChangeCursorOnHover(viewModeButton, GameCursor.SELECT, messageDispatcher));
			viewModeButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary, "MediumHover", "ConfirmMedium"));
			viewModeButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, viewMode);
					rebuildUI();
				}
			});

			tooltipFactory.simpleTooltip(viewModeButton, viewMode.getI18nKey(), TooltipLocationHint.BELOW);

			viewModeGroup.add(viewModeButton);
			viewModeButtons.add(viewModeButton);
		}
		viewModeGroup.setMinCheckCount(1);
		viewModeGroup.setUncheckLast(true);

	}

	@Override
	public void populate(Table containerTable) {
		update();
		containerTable.add(this.layoutTable);
	}

	@Override
	public void update() {
		if (gameContext != null) {
			timeDateWidget.update(gameContext);
		}
	}


	@Override
	public GuiViewName getName() {
		// This is a special case GuiView which lives outside of the normal usage
		return GuiViewName.TIME_DATE;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SETTLEMENT_SPAWNED: {
				reset(gameContext);
				return true;
			}
			case MessageType.GUI_VIEW_MODE_CHANGED: {
				rebuildUI();
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public void setHiddenGuiAreas(Set<GuiArea> hiddenGuiAreas) {
		this.hiddenGuiAreas = hiddenGuiAreas;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		reset(gameContext);
	}

	@Override
	public void clearContextRelatedState() {
		this.hiddenGuiAreas.clear();
	}

}
