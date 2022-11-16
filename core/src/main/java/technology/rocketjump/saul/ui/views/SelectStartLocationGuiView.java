package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
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
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

@Singleton
public class SelectStartLocationGuiView implements GuiView, GameContextAware {

	private Button confirmEmbarkButton;
	private Table containerTable;
	private GameContext gameContext;
	private boolean hidden;

	@Inject
	private SelectStartLocationGuiView(GuiSkinRepository skinRepository, MessageDispatcher messageDispatcher,
									   TooltipFactory tooltipFactory) {
		Skin skin = skinRepository.getMainGameSkin();

		confirmEmbarkButton = new Button(skin.getDrawable("icon_begin"));
		confirmEmbarkButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (gameContext.getAreaMap().getEmbarkPoint() != null) {
					SelectStartLocationGuiView.this.hidden = true;
					gameContext.getSettlementState().setGameState(GameState.STARTING_SPAWN);
					messageDispatcher.dispatchMessage(MessageType.BEGIN_SPAWN_SETTLEMENT);
				}
			}
		});
		confirmEmbarkButton.addListener(new ChangeCursorOnHover(confirmEmbarkButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(confirmEmbarkButton, "GUI.EMBARK.START", TooltipLocationHint.ABOVE);
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
}
