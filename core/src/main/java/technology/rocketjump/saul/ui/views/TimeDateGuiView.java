package technology.rocketjump.saul.ui.views;

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
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.gamecontext.GameState;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.screens.ManagementScreenName;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.maingame.TimeDateWidget;

@Singleton
public class TimeDateGuiView implements GuiView, GameContextAware, Telegraph, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final Skin skin;
	private final Table layoutTable;
	private final Table managementScreenButtonTable;
	private GameContext gameContext;

	private final TimeDateWidget timeDateWidget;

	@Inject
	public TimeDateGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						   TimeDateWidget timeDateWidget,
						   TooltipFactory tooltipFactory) {
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		Skin uiSkin = guiSkinRepository.getDefault();
		this.skin = guiSkinRepository.getMainGameSkin();

		this.timeDateWidget = timeDateWidget;

		managementScreenButtonTable = new Table(uiSkin);
		managementScreenButtonTable.padTop(19);
		managementScreenButtonTable.padRight(26);
		managementScreenButtonTable.defaults().padLeft(11);

		layoutTable = new Table(uiSkin);
		reset(null);

		messageDispatcher.addListener(this, MessageType.SETTLEMENT_SPAWNED);
	}

	private void reset(GameContext gameContext) {
		layoutTable.clearChildren();
		if (gameContext == null || !gameContext.getSettlementState().getGameState().equals(GameState.SELECT_SPAWN_LOCATION)) {
			layoutTable.add(managementScreenButtonTable).right().top();
			layoutTable.add(timeDateWidget).top().right().padTop(6);
		}
	}

	@Override
	public void rebuildUI() {
		managementScreenButtonTable.clearChildren();

		for (ManagementScreenName managementScreen : ManagementScreenName.managementScreensOrderedForUI) {
			Button screenButton = new Button(skin, managementScreen.buttonStyleName);

			screenButton.addListener(new ChangeCursorOnHover(GameCursor.SELECT, messageDispatcher));

			screenButton.addListener(new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, managementScreen.name());
				}
			});
			tooltipFactory.simpleTooltip(screenButton, managementScreen.titleI18nKey, TooltipLocationHint.BELOW);
			managementScreenButtonTable.add(screenButton).size(157f/2f,170f/2f);
		}
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
		return null;
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
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		reset(gameContext);
	}

	@Override
	public void clearContextRelatedState() {

	}
}
