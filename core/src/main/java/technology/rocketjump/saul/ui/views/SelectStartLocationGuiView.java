package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.gamecontext.GameState;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.ui.widgets.ButtonStyle;
import technology.rocketjump.saul.ui.widgets.IconButton;
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;

@Singleton
public class SelectStartLocationGuiView implements GuiView, GameContextAware {

	private IconButton confirmEmbarkButton;
	private Table containerTable;
	private GameContext gameContext;
	private boolean hidden;

	@Inject
	private SelectStartLocationGuiView(IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher) {
		confirmEmbarkButton = iconButtonFactory.create("GUI.EMBARK.START", "flying-flag", HexColors.POSITIVE_COLOR, ButtonStyle.DEFAULT);
		confirmEmbarkButton.setAction(() -> {
			if (gameContext.getAreaMap().getEmbarkPoint() != null) {
				this.hidden = true;
				gameContext.getSettlementState().setGameState(GameState.STARTING_SPAWN);
				messageDispatcher.dispatchMessage(MessageType.BEGIN_SPAWN_SETTLEMENT);
			}
		});
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
				containerTable.add(confirmEmbarkButton).pad(5);
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
}
