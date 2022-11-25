package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.components.creature.NeedsComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rendering.utils.ColorMixer;
import technology.rocketjump.saul.settlement.SettlerTracker;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.*;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class SettlerManagementScreen extends AbstractGameScreen implements DisplaysText, GameContextAware {
	private static final Predicate<Entity> IS_CIVILIAN = settler -> {
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
		return militaryComponent == null || militaryComponent.getSquadId() == null;
	};
	private static final Predicate<Entity> IS_MILITARY = IS_CIVILIAN.negate();

	private record MatchesActiveProfession(Skill skill) implements Predicate<Entity> {

		@Override
			public boolean test(Entity entity) {
				SkillsComponent skillsComponent = entity.getComponent(SkillsComponent.class);
				if (skillsComponent != null) {
					for (SkillsComponent.QuantifiedSkill activeProfession : skillsComponent.getActiveProfessions()) {
						if (skill.equals(activeProfession.getSkill())) {
							return true;
						}
					}
				}
				return false;
			}
		}

	private static final Comparator<Entity> SORT_HAPPINESS = Comparator.comparingInt(settler -> settler.getComponent(HappinessComponent.class).getNetModifier());
	private static final Comparator<Entity> SORT_NAME = Comparator.comparing(SettlerManagementScreen::getName);
	private static final Comparator<Entity> SORT_MILITARY_CIVILIAN = Comparator.comparing((Function<Entity, Long>) settler -> {
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
		if (militaryComponent != null && militaryComponent.getSquadId() != null) {
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
	private final TooltipFactory tooltipFactory;
	private final SettlerProfessionFactory settlerProfessionFactory;

	private GameContext gameContext;
	private Stack stack;
	private ScrollPane scrollPane;
	private Label filterNameLabel;
	private Label filterCountLabel;
	private String searchBarText = "";
	private Comparator<Entity> selectedSortFunction = SORT_HAPPINESS;
	private Predicate<Entity> selectedFilter;

	@Inject
	public SettlerManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
	                               I18nTranslator i18nTranslator, LabelFactory labelFactory, ButtonFactory buttonFactory,
	                               SkillDictionary skillDictionary, SettlerTracker settlerTracker, EntityRenderer entityRenderer,
	                               TooltipFactory tooltipFactory, SettlerProfessionFactory settlerProfessionFactory) {
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.i18nTranslator = i18nTranslator;
		this.labelFactory = labelFactory;
		this.buttonFactory = buttonFactory;
		this.skillDictionary = skillDictionary;
		this.messageDispatcher = messageDispatcher;
		this.settlerTracker = settlerTracker;
		this.entityRenderer = entityRenderer;
		this.tooltipFactory = tooltipFactory;
		this.settlerProfessionFactory = settlerProfessionFactory;
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

		filterCountLabel = new Label("", managementSkin, "sort_by_label");
		filterCountLabel.setAlignment(Align.left);

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
		professionFilterButton(professionButtonGroup, professionButtons, "settlers_all", "GUI.SETTLER_MANAGEMENT.PROFESSION.CIVILIAN", IS_CIVILIAN);
		professionFilterButton(professionButtonGroup, professionButtons, "settlers_military", "GUI.SETTLER_MANAGEMENT.PROFESSION.MILITARY", IS_MILITARY);
		for (Skill profession : skillDictionary.getAllProfessions()) {
			professionFilterButton(professionButtonGroup, professionButtons, profession.getIcon(), profession.getI18nKey(), new MatchesActiveProfession(profession));
		}
		professionFilterButton(professionButtonGroup, professionButtons, "settlers_job_villager", "GUI.SETTLER_MANAGEMENT.PROFESSION.VILLAGER", new MatchesActiveProfession(SkillDictionary.NULL_PROFESSION));

		TextField searchBar = new TextField("", managementSkin, "search_bar_input");
		searchBar.setText(searchBarText);
		searchBar.setMessageText(i18nTranslator.translate("GUI.RESOURCE_MANAGEMENT.SEARCH"));
		searchBar.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if (event.getKeyCode() == Input.Keys.ESCAPE) {
					stage.setKeyboardFocus(null);
					return false;
				}
				return super.keyDown(event, keycode);
			}

			@Override
			public boolean keyTyped(InputEvent event, char character) {
				searchBarText = searchBar.getText();
				rebuildSettlerTable();
				return true;
			}
		});
		buttonFactory.attachClickCursor(searchBar, GameCursor.I_BEAM);

		ButtonGroup<Button> sortByButtonGroup = new ButtonGroup<>();
		Label sortByLabel  = new Label(i18nTranslator.translate("GUI.SETTLER_MANAGEMENT.SORT_BY"), managementSkin, "sort_by_label");
		Button sortByHappiness = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.HAPPINESS", SORT_HAPPINESS);
		Button sortByName = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.NAME", SORT_NAME);
		Button sortBySkillLevel = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.SKILL_LEVEL", Comparator.comparing((Function<Entity, Float>) settler -> {
			SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
			java.util.List<SkillsComponent.QuantifiedSkill> activeProfessions = skillsComponent.getActiveProfessions();

			if (selectedFilter instanceof MatchesActiveProfession matchesProfessionFilter) {
				activeProfessions = activeProfessions.stream().filter(quantifiedSkill -> matchesProfessionFilter.skill.equals(quantifiedSkill.getSkill())).toList();
			}
			if (activeProfessions.isEmpty()) {
				return 0.0f;
			} else {
				SkillsComponent.QuantifiedSkill quantifiedSkill = activeProfessions.get(0);
				return quantifiedSkill.getLevel() + skillsComponent.getNextLevelProgressPercent(quantifiedSkill.getSkill());
			}
		}).reversed());
		Button sortByMilitaryCivilian = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.MILITARY_CIVILIAN", SORT_MILITARY_CIVILIAN);
		sortByButtonGroup.add(sortByHappiness, sortByName, sortBySkillLevel, sortByMilitaryCivilian);

		Table filterLabels = new Table();
		filterLabels.add(filterNameLabel).left().spaceRight(30f);
		filterLabels.add(filterCountLabel).left().growX().bottom();
		Table filters = new Table();
		filters.defaults().bottom();
		filters.add(filterLabels).width(600f).spaceRight(70f);
		filters.add(searchBar).width(524).spaceRight(70f);
		filters.add(sortByLabel).spaceRight(56f);
		filters.add(sortByHappiness).spaceRight(38);
		filters.add(sortByName).spaceRight(38);
		filters.add(sortBySkillLevel).spaceRight(38);
		filters.add(sortByMilitaryCivilian).spaceRight(38);

		Table table = new Table();
		table.add(titleLabel).row();
		table.add(professionButtons).row();
		table.add(filters).left().row();
		table.add(new Image(managementSkin.getDrawable("asset_line"))).padTop(40f).row();
		table.add(scrollPane).row();
		return table;
	}

	private void professionFilterButton(ButtonGroup<ImageButton> professionButtonGroup, Table professionButtons, String drawableName, String i18nKey, Predicate<Entity> filter) {
		Drawable drawable = managementSkin.getDrawable(drawableName);
		ImageButton button = buttonFactory.checkableButton(drawable);
		button.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (button.isChecked()) {
					filterNameLabel.setText(i18nTranslator.translate(i18nKey));
					selectedFilter = filter;
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
				}
				rebuildSettlerTable();
			}
		});
		buttonFactory.attachClickCursor(button, GameCursor.SELECT);
		return button;
	}

	private void rebuildSettlerTable() {
		Collection<Entity> settlers = settlerTracker.getLiving()
				.stream()
				.filter(selectedFilter)
				.filter(settler -> getName(settler).toLowerCase().contains(searchBarText.toLowerCase()))
				.sorted(selectedSortFunction)
				.collect(Collectors.toList());

		filterCountLabel.setText(settlers.size());

		Table settlersTable = new Table();
		for (Entity settler : settlers) {
			Table mugshotColumn = mugshot(settler);
			Table textSummaryColumn = textSummary(settler);
			Table happinessColumn = happiness(settler);
			Table needsColumn = needs(settler);
			Table professionsColumn = professions(settler);

			addGotoSettlerBehaviour(mugshotColumn, settler);

			settlersTable.add(mugshotColumn).spaceRight(50f);
			settlersTable.add(textSummaryColumn).fillX().spaceRight(50f);
			settlersTable.add(happinessColumn).spaceRight(50f);
			settlersTable.add(needsColumn).spaceRight(50f);
			settlersTable.add(professionsColumn).spaceRight(50f).spaceBottom(76f).spaceTop(38f);

			settlersTable.left().row();
		}

		scrollPane.setActor(settlersTable);
	}

	private void addGotoSettlerBehaviour(Table mugshotColumn, Entity settler) {
		mugshotColumn.setTouchable(Touchable.enabled);
		mugshotColumn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Vector2 position = settler.getLocationComponent().getWorldOrParentPosition();
				messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
				messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, position);
				messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(settler, 0));
			}
		});
		buttonFactory.attachClickCursor(mugshotColumn, GameCursor.SELECT);
	}

	private Table professions(Entity settler) {
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
		if (skillsComponent == null) {
			return new Table();
		}

		Table table = new Table();
		settlerProfessionFactory.addProfessionComponents(settler, table);
		return table;
	}


	private Table needs(Entity settler) {
		Table table = new Table();
		table.defaults().spaceBottom(30f);
		NeedsComponent needsComponent = settler.getComponent(NeedsComponent.class);
		if (needsComponent != null) {
			for (Map.Entry<EntityNeed, Double> entry : needsComponent.getAll()) {
				EntityNeed need = entry.getKey();
				Double needValue = entry.getValue();
				if (needValue != null) {
					Label needLabel = new Label(i18nTranslator.translate(need.getI18nKey()), managementSkin, "item_type_name_label");
					ProgressBar progressBar = new ProgressBar((float) NeedsComponent.MIN_NEED_VALUE, (float) NeedsComponent.MAX_NEED_VALUE, 1, false, managementSkin);
					progressBar.setValue(Math.round(needValue));
					progressBar.setDisabled(true);
					progressBar.setWidth(318);
					progressBar.setHeight(42);
					Color progressBarColour;
					if (needValue >= 55f) {
						progressBarColour = ColorMixer.interpolate(55f, (float) NeedsComponent.MAX_NEED_VALUE, needValue.floatValue(), managementSkin.getColor("progress_bar_yellow"), managementSkin.getColor("progress_bar_green"));
					} else {
						progressBarColour = ColorMixer.interpolate((float) NeedsComponent.MIN_NEED_VALUE, 55f, needValue.floatValue(), managementSkin.getColor("progress_bar_red"), managementSkin.getColor("progress_bar_yellow"));
					}
					ProgressBar.ProgressBarStyle clonedStyle = new ProgressBar.ProgressBarStyle(progressBar.getStyle());
					if (clonedStyle.knobBefore instanceof NinePatchDrawable ninePatchDrawable) {
						clonedStyle.knobBefore = ninePatchDrawable.tint(progressBarColour);
					}
					progressBar.setStyle(clonedStyle);


					table.add(needLabel).right().spaceRight(28);
					table.add(progressBar).left().width(318).height(42);
					table.row();
				}
			}

		}
		return table;
	}

	private Table happiness(Entity settler) {
		HappinessComponent happinessComponent = settler.getComponent(HappinessComponent.class);
		int netHappiness = happinessComponent.getNetModifier();
		String netHappinessString = (netHappiness > 0 ? "+" : "") + netHappiness;
		String happinessText = i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.HAPPINESS", Map.of(
				"happinessValue", new I18nWord(netHappinessString)
		)).toString();
		Label happinessLabel = tableLabel(happinessText);

		Table modifiersTable = new Table();
		int modifierCount = 1;
		for (HappinessComponent.HappinessModifier modifier : happinessComponent.currentModifiers()) {
			int modifierAmount = modifier.modifierAmount;
			final String drawableName;
			if (modifierAmount > 0) {
				drawableName = "icon_happy";
			} else {
				drawableName = "icon_sad";
			}
			Image modifierImage = new Image(managementSkin.getDrawable(drawableName));
			tooltipFactory.simpleTooltip(modifierImage, modifier.getI18nKey(), TooltipLocationHint.BELOW);

			modifiersTable.add(modifierImage).space(10f);

			if (modifierCount % 5 == 0) {
				modifiersTable.row();
			}
			modifierCount++;
		}



		Table table = new Table();
		table.add(happinessLabel).row();
		table.add(modifiersTable);



		return table;
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
		Label nameLabel = tableLabel(settlerName);
		nameLabel.setAlignment(Align.left);
		table.add(nameLabel).growX().row();

		for (String behaviourDescription : behaviourDescriptions) {
			Label descriptionLabel = tableLabel(behaviourDescription);
			descriptionLabel.setAlignment(Align.left);
			table.add(descriptionLabel).growX().row();
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

	private Label tableLabel(String text) {
		return new Label(text, managementSkin, "table_value_label");
	}
}
