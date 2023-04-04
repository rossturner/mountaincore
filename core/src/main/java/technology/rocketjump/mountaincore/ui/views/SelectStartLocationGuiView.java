package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.gamecontext.GameState;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;

@Singleton
public class SelectStartLocationGuiView implements GuiView, GameContextAware, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final ButtonFactory buttonFactory;
	private Button confirmEmbarkButton;
	private Table containerTable;
	private GameContext gameContext;
	private boolean hidden;

	@Inject
	private SelectStartLocationGuiView(MessageDispatcher messageDispatcher, ButtonFactory buttonFactory) {
		this.messageDispatcher = messageDispatcher;
		this.buttonFactory = buttonFactory;

		rebuildUI();
	}

	@Override
	public void populate(Table containerTable) {
		this.containerTable = containerTable;
	}

	@Override
	public void update() {
		if (containerTable != null && gameContext != null) {
			containerTable.clearChildren();
			if (gameContext.getAreaMap().getEmbarkPoint() != null && !hidden) {
				containerTable.add(confirmEmbarkButton).pad(20);
			}
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.SELECT_STARTING_LOCATION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.SELECT_STARTING_LOCATION;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.hidden = false;
	}

	@Override
	public void rebuildUI() {

		confirmEmbarkButton = buttonFactory.buildDrawableButton("icon_begin", "GUI.EMBARK.START", () -> {
			if (gameContext.getAreaMap().getEmbarkPoint() != null) {
				SelectStartLocationGuiView.this.hidden = true;
				gameContext.getSettlementState().setGameState(GameState.STARTING_SPAWN);
				messageDispatcher.dispatchMessage(MessageType.BEGIN_SPAWN_SETTLEMENT);
			}
		});
	}
}
