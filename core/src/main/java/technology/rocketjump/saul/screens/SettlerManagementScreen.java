package technology.rocketjump.saul.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;

import javax.inject.Singleton;

@Singleton
public class SettlerManagementScreen extends AbstractGameScreen implements DisplaysText {
	private Stack stack;
	private final MenuSkin menuSkin;

	@Inject
	public SettlerManagementScreen(GuiSkinRepository guiSkinRepository) {
		this.menuSkin = guiSkinRepository.getMenuSkin();
	}

	@Override
	public String getName() {
		return ManagementScreenName.SETTLERS.name();
	}

	@Override
	public void show() {
		rebuildUI();
	}

	@Override
	public void dispose() {

	}

	@Override
	public void rebuildUI() {
		stack = new Stack();
		stack.setFillParent(true);
		stack.add(menuSkin.buildBackgroundBaseLayer());
		stack.add(menuSkin.buildPaperLayer(new Table()));

		stage.addActor(stack);
	}
}
