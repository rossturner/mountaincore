package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;

public abstract class PaperMenu implements Menu {
	protected final MenuSkin skin;
	protected final Stack stack = new Stack();

	public PaperMenu(GuiSkinRepository skinRepository) {
		this.skin = skinRepository.getMenuSkin();
	}

	@Override
	public abstract void show();

	@Override
	public void hide() {
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(stack).grow();
	}

	@Override
	public void reset() {
	}

	public abstract void savedGamesUpdated();

	protected abstract Actor buildComponentLayer();

	public void rebuild() {
		stack.addActor(skin.buildBackgroundBaseLayer());
		stack.addActor(skin.buildPaperLayer(buildComponentLayer(), 257, false));
		savedGamesUpdated();
	}
}
