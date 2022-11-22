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
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.settlement.SettlerTracker;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;
import technology.rocketjump.saul.ui.widgets.EnhancedScrollPane;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;
import technology.rocketjump.saul.ui.widgets.LabelFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

@Singleton
public class SettlerManagementScreen extends AbstractGameScreen implements DisplaysText, GameContextAware {
	private static final Comparator<Entity> SORT_HAPPINESS = Comparator.comparingInt(settler -> settler.getComponent(HappinessComponent.class).getNetModifier());
	private static final Comparator<Entity> SORT_NAME = Comparator.comparing(SettlerManagementScreen::getName);
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

	private static String getName(Entity settler) {
		return ((CreatureEntityAttributes) settler.getPhysicalEntityComponent().getAttributes()).getName().toString();
	}

	private final MessageDispatcher messageDispatcher;
	private final ManagementSkin managementSkin;
	private final MenuSkin menuSkin;
	private final I18nTranslator i18nTranslator;
	private final LabelFactory labelFactory;
	private final ButtonFactory buttonFactory;
	private final SkillDictionary skillDictionary;
	private final SettlerTracker settlerTracker;
	private final EntityRenderer entityRenderer;

	private GameContext gameContext;
	private Stack stack;
	private ScrollPane scrollPane;
	private Label filterNameLabel;
	private Comparator<Entity> selectedSortFunction;

	@Inject
	public SettlerManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
	                               I18nTranslator i18nTranslator, LabelFactory labelFactory, ButtonFactory buttonFactory,
	                               SkillDictionary skillDictionary, SettlerTracker settlerTracker, EntityRenderer entityRenderer) {
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.i18nTranslator = i18nTranslator;
		this.labelFactory = labelFactory;
		this.buttonFactory = buttonFactory;
		this.skillDictionary = skillDictionary;
		this.messageDispatcher = messageDispatcher;
		this.settlerTracker = settlerTracker;
		this.entityRenderer = entityRenderer;
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

		scrollPane = new EnhancedScrollPane(null, menuSkin);
		scrollPane.setForceScroll(false, true);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollbarsVisible(true);
		scrollPane.setScrollBarPositions(true, true);

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
		table.add(new Image(managementSkin.getDrawable("asset_resources_line"))).row();
		table.add(scrollPane).row();
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
					rebuildSettlerTable();
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
				rebuildSettlerTable();
			}
		});
		buttonFactory.attachClickCursor(button, GameCursor.SELECT);
		return button;
	}

	private void rebuildSettlerTable() {
		Collection<Entity> settlers = settlerTracker.getLiving();
		//TODO: apply filters and sorts here

		Table settlersTable = new Table();
		//TODO: most of these components should be reusable on entity selection views
		for (Entity settler : settlers) {
			Table mugshotColumn = mugshot(settler);
			Table textSummaryColumn = textSummary(settler);


			Table row = new Table();
			row.debugAll();
			row.add(mugshotColumn).spaceRight(50f);
			row.add(textSummaryColumn).fillX().spaceRight(50f);

			settlersTable.add(row).left().uniformX().row();
		}

		scrollPane.setActor(settlersTable);
	}

	private Table textSummary(Entity settler) {
		String settlerName = getName(settler);
		java.util.List<String> behaviourDescriptions = new ArrayList<>();
		//TODO: if in military, get rank
		if (settler.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			java.util.List<I18nText> description = creatureBehaviour.getDescription(i18nTranslator, gameContext, messageDispatcher);
			for (I18nText i18nText : description) {
				behaviourDescriptions.add(i18nText.toString());
			}
		}

		Table table = new Table();
		Label nameLabel = new Label(settlerName, managementSkin, "table_value_label");
		nameLabel.setAlignment(Align.left);
		table.add(nameLabel).fillX().row();

		for (String behaviourDescription : behaviourDescriptions) {
			Label descriptionLabel = new Label(behaviourDescription, managementSkin, "table_value_label");
			descriptionLabel.setAlignment(Align.left);
			table.add(descriptionLabel).fillX().row();
		}

		return table;
	}

	private Table mugshot(Entity settler) {
		Table table = new Table();
		float scaleFactor = 0.9f;
		Drawable background = managementSkin.bgForExampleEntity(settler.getId());
		table.setBackground(background);
		EntityDrawable entityDrawable = new EntityDrawable(settler, entityRenderer, true, messageDispatcher);
		entityDrawable.setMinSize(background.getMinWidth() * scaleFactor, background.getMinHeight()  * scaleFactor);
		table.add(new Image(entityDrawable));
		return table;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
