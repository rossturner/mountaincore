package technology.rocketjump.saul.screens;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.LabelFactory;

import javax.inject.Singleton;

@Singleton
public class SettlerManagementScreen extends AbstractGameScreen implements DisplaysText {
	private Stack stack;
	private final MenuSkin menuSkin;
	private final LabelFactory labelFactory;

	@Inject
	public SettlerManagementScreen(GuiSkinRepository guiSkinRepository, LabelFactory labelFactory) {
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.labelFactory = labelFactory;
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
		stack.add(menuSkin.buildPaperLayer(buildPaperComponents()));

		stage.addActor(stack);
	}

	private Actor buildPaperComponents() {
		Label titleLabel = labelFactory.titleRibbon("GUI.SETTLER_MANAGEMENT.TITLE");

		Table table = new Table();
		table.add(titleLabel).row();


		return table;
	}
}
