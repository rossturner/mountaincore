package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

public abstract class PaperMenu implements Menu {
	protected final Skin skin;
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

	private Actor buildBackgroundAndComponents() {
		Table table = new Table();
		table.setName("background");
		table.setBackground(skin.getDrawable("paper_texture_bg"));
		table.add(new Image(skin.getDrawable("paper_texture_bg_pattern_large"))).growY().padLeft(242.0f);
		table.add(buildComponentLayer()).expandX().fill();
		table.add(new Image(skin.getDrawable("paper_texture_bg_pattern_large"))).growY().padRight(242.0f);
		return table;
	}

	private Actor buildBackgroundBaseLayer() {
		Table table = new Table();
		table.setName("backgroundBase");
		table.add(new Image(skin.getDrawable("menu_bg_left"))).left();
		table.add().expandX();
		table.add(new Image(skin.getDrawable("menu_bg_right"))).right();
		return table;
	}

	public void rebuild() {
		stack.addActor(buildBackgroundBaseLayer());
		stack.addActor(buildBackgroundAndComponents());
		savedGamesUpdated();
	}
}
