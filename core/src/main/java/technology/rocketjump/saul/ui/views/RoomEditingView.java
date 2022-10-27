package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.ui.i18n.DisplaysText;

@Singleton
public class RoomEditingView implements GuiView, GameContextAware, DisplaysText {

	private GameContext gameContext;

	@Inject
	public RoomEditingView() {

	}

	@Override
	public void rebuildUI() {

	}

	@Override
	public void populate(Table containerTable) {

	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	@Override
	public void update() {

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ROOM_EDITING;
	}

	@Override
	public GuiViewName getParentViewName() {
		// TODO if room selected, parent view name is ROOM_SELECTION, but otherwise is default
		return GuiViewName.ROOM_SELECTION;
	}
}
