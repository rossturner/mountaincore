package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;
import technology.rocketjump.saul.ui.widgets.LabelFactory;

import javax.inject.Singleton;
import java.util.Comparator;
import java.util.function.Function;

@Singleton
public class SettlerManagementScreen extends AbstractGameScreen implements DisplaysText {
	private static final Comparator<Entity> SORT_HAPPINESS = Comparator.comparingInt(settler -> settler.getComponent(HappinessComponent.class).getNetModifier());
	private static final Comparator<Entity> SORT_NAME = Comparator.comparing(settler -> ((CreatureEntityAttributes)settler.getPhysicalEntityComponent().getAttributes()).getName().toString());
	private static final Comparator<Entity> SORT_SKILL_LEVEL = Comparator.comparing(settler -> {
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
		java.util.List<SkillsComponent.QuantifiedSkill> activeProfessions = skillsComponent.getActiveProfessions();
		if (activeProfessions.isEmpty()) {
			return 0;
		} else {
			return activeProfessions.get(0).getLevel();
		}
	});
	private static final Comparator<Entity> SORT_MILITARY_CIVILIAN = Comparator.comparing((Function<Entity, Long>) settler -> {
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
		if (militaryComponent != null) {
			return militaryComponent.getSquadId();
		} else {
			return Long.MAX_VALUE;
		}
	}).thenComparing(SORT_NAME);

	private final MessageDispatcher messageDispatcher;
	private final Skin managementSkin;
	private final MenuSkin menuSkin;
	private final I18nTranslator i18nTranslator;
	private final LabelFactory labelFactory;
	private final ButtonFactory buttonFactory;
	private final SkillDictionary skillDictionary;

	private Stack stack;
	private Label filterNameLabel;
	private Comparator<Entity> selectedSortFunction;

	@Inject
	public SettlerManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
	                               I18nTranslator i18nTranslator, LabelFactory labelFactory, ButtonFactory buttonFactory,
	                               SkillDictionary skillDictionary) {
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.i18nTranslator = i18nTranslator;
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
		filterNameLabel = new Label("", managementSkin, "stockpile_group_filter_label"); //probably should be scaled to fit label
		filterNameLabel.setAlignment(Align.left);

		stack = new Stack();
		stack.setFillParent(true);
		stack.add(menuSkin.buildBackgroundBaseLayer());
		stack.add(menuSkin.buildPaperLayer(buildPaperComponents()));

		stage.addActor(stack);
	}

	private Actor buildPaperComponents() {
		Label titleLabel = labelFactory.titleRibbon("GUI.SETTLER_MANAGEMENT.TITLE");

		//TODO: consider a horizontal scrollbar for when more than designed professions exist
		Table professionButtons = new Table();
		ButtonGroup<ImageButton> professionButtonGroup = new ButtonGroup<>();

		professionFilterButton(professionButtonGroup, professionButtons, "settlers_all", "GUI.SETTLER_MANAGEMENT.PROFESSION.CIVILIAN");
		professionFilterButton(professionButtonGroup, professionButtons, "settlers_military", "GUI.SETTLER_MANAGEMENT.PROFESSION.MILITARY");
		for (Skill profession : skillDictionary.getAllProfessions()) {
			professionFilterButton(professionButtonGroup, professionButtons, profession.getIcon(), profession.getI18nKey());
		}
		professionFilterButton(professionButtonGroup, professionButtons, "settlers_job_villager", "GUI.SETTLER_MANAGEMENT.PROFESSION.VILLAGER");


		Label sortByLabel  = new Label(i18nTranslator.translate("GUI.SETTLER_MANAGEMENT.SORT_BY"), managementSkin, "sort_by_label");
		Button sortByHappiness = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.HAPPINESS", SORT_HAPPINESS);
		Button sortByName = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.NAME", SORT_NAME);
		Button sortBySkillLevel = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.SKILL_LEVEL", SORT_SKILL_LEVEL);
		Button sortByMilitaryCivilian = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.MILITARY_CIVILIAN", SORT_MILITARY_CIVILIAN);
		new ButtonGroup<>(sortByHappiness, sortByName, sortBySkillLevel, sortByMilitaryCivilian);

		Table filters = new Table();
		filters.defaults().growX();
		filters.add(filterNameLabel).width(400f).padLeft(8f);
//		filters.add(searchBar).width(524);
		filters.add(sortByLabel);
		filters.add(sortByHappiness);
		filters.add(sortByName);
		filters.add(sortBySkillLevel);
		filters.add(sortByMilitaryCivilian);


		Table table = new Table();
		table.add(titleLabel).row();
		table.add(professionButtons).row();
		table.add(filters).growX().row();

		return table;
	}

	private void professionFilterButton(ButtonGroup<ImageButton> professionButtonGroup, Table professionButtons, String drawableName, String i18nKey) {
		Drawable drawable = managementSkin.getDrawable(drawableName);
		ImageButton button = buttonFactory.checkableButton(drawable);
		button.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (button.isChecked()) {
					filterNameLabel.setText(i18nTranslator.translate(i18nKey));
					//
					//				rebuildSettlerTable();
				}
			}
		});

		professionButtons.add(button);
		professionButtonGroup.add(button);
	}


	//Very similar to ResourceManagementScreen's method
	private Button buildTextSortButton(String i18nKey, Comparator<Entity> sortFunction) {
		ImageTextButton button = new ImageTextButton(i18nTranslator.translate(i18nKey), managementSkin, "sort_by_button");
		button.defaults().padRight(9f).spaceLeft(12f);
		Image image = button.getImage(); //Swap actors or cells doesn't work, absolute agony
		button.removeActor(image);
		button.add(image);
		button.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (button.isChecked()) {
					selectedSortFunction = sortFunction;
				} else {
					selectedSortFunction = null;
				}
//				rebuildSettlerTable();
			}
		});
		buttonFactory.attachClickCursor(button, GameCursor.SELECT);
		return button;
	}
}
