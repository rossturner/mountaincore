package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;
import technology.rocketjump.saul.ui.widgets.LabelFactory;

import javax.inject.Singleton;

@Singleton
public class SettlerManagementScreen extends AbstractGameScreen implements DisplaysText {
	private Stack stack;
	private final MessageDispatcher messageDispatcher;
	private final Skin mainGameSkin;
	private final MenuSkin menuSkin;
	private final LabelFactory labelFactory;
	private final ButtonFactory buttonFactory;
	private final SkillDictionary skillDictionary;

	@Inject
	public SettlerManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
	                               LabelFactory labelFactory, ButtonFactory buttonFactory, SkillDictionary skillDictionary) {
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.labelFactory = labelFactory;
		this.buttonFactory = buttonFactory;
		this.skillDictionary = skillDictionary;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public String getName() {
		return ManagementScreenName.SETTLERS.name();
	}

	@Override
	public void show() {
		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new ManagementScreenInputHandler(messageDispatcher));
		Gdx.input.setInputProcessor(inputMultiplexer);

		rebuildUI();

		stage.setKeyboardFocus(null);
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

		Table professionButtons = new Table();

		//TODO: consider a horizontal scrollbar for when more than designed professions exist
		for (Skill profession : skillDictionary.getAllProfessions()) {
			Drawable drawable = mainGameSkin.getDrawable(profession.getIcon());
			ImageButton button = buttonFactory.checkableButton(drawable);

			professionButtons.add(button);
		}



		Table table = new Table();
		table.add(titleLabel).row();
		table.add(professionButtons).row();

		return table;
	}
}
