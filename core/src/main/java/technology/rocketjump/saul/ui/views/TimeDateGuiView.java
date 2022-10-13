package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.gamecontext.GameState;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.screens.GameScreenDictionary;
import technology.rocketjump.saul.screens.ManagementScreen;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nTextButton;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.saul.ui.widgets.maingame.TimeDateWidget;

@Singleton
public class TimeDateGuiView implements GuiView, GameContextAware, Telegraph {

	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final I18nWidgetFactory i18nWidgetFactory;
	private Table layoutTable;
	private Table timeDateTable;
	private Table managementScreenButtonTable;
	private GameContext gameContext;

	private final TimeDateWidget timeDateWidget;

	@Inject
	public TimeDateGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						   I18nTranslator i18nTranslator, TimeDateWidget timeDateWidget,
						   I18nWidgetFactory i18nWidgetFactory) {
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;
		Skin uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;

		timeDateTable = new Table(uiSkin);
		timeDateTable.background("default-rect");
		timeDateTable.pad(5);

		this.timeDateWidget = timeDateWidget;

		managementScreenButtonTable = new Table(uiSkin);

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


	public void init(GameScreenDictionary gameScreenDictionary) {
		managementScreenButtonTable.clearChildren();

		for (ManagementScreen managementScreen : gameScreenDictionary.getAllManagementScreens()) {
			I18nTextButton screenButton = i18nWidgetFactory.createTextButton(managementScreen.getTitleI18nKey());
			screenButton.addListener(new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, managementScreen.getName());
				}
			});
			managementScreenButtonTable.add(screenButton).pad(2);
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
