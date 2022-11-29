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
import technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.components.creature.NeedsComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.components.furniture.SleepingPositionComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rendering.utils.ColorMixer;
import technology.rocketjump.saul.settlement.SettlementFurnitureTracker;
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
import java.util.function.Consumer;
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
				if (IS_CIVILIAN.test(entity) && skillsComponent != null) {
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
	private final WidgetFactory widgetFactory;
	private final SkillDictionary skillDictionary;
	private final SettlerTracker settlerTracker;
	private final EntityRenderer entityRenderer;
	private final TooltipFactory tooltipFactory;
	private final SettlerProfessionFactory settlerProfessionFactory;
	private final SettlementFurnitureTracker settlementFurnitureTracker;

	private GameContext gameContext;
	private Stack stack;
	private ScrollPane scrollPane;
	private Label filterNameLabel;
	private Label filterCountLabel;
	private Label populationStatisticsLabel;
	private String searchBarText = "";
	private Comparator<Entity> selectedSortFunction = SORT_HAPPINESS;
	private Predicate<Entity> selectedFilter;

	@Inject
	public SettlerManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
	                               I18nTranslator i18nTranslator, LabelFactory labelFactory, ButtonFactory buttonFactory,
	                               WidgetFactory widgetFactory, SkillDictionary skillDictionary, SettlerTracker settlerTracker,
	                               EntityRenderer entityRenderer, TooltipFactory tooltipFactory, SettlerProfessionFactory settlerProfessionFactory,
	                               SettlementFurnitureTracker settlementFurnitureTracker) {
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.i18nTranslator = i18nTranslator;
		this.labelFactory = labelFactory;
		this.buttonFactory = buttonFactory;
		this.widgetFactory = widgetFactory;
		this.skillDictionary = skillDictionary;
		this.messageDispatcher = messageDispatcher;
		this.settlerTracker = settlerTracker;
		this.entityRenderer = entityRenderer;
		this.tooltipFactory = tooltipFactory;
		this.settlerProfessionFactory = settlerProfessionFactory;
		this.settlementFurnitureTracker = settlementFurnitureTracker;
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
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
		stack.add(menuSkin.buildPaperLayer(buildPaperComponents(), 136, true));
		stack.add(buildExitTable(136 + menuSkin.getDrawable("paper_texture_bg_pattern_thin").getMinWidth() + 5f));

		stage.addActor(stack);
	}


	private Actor buildExitTable(float leftPadding) {
		Table table = new Table();
		Button exitButton = new Button(menuSkin, "btn_exit");
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
			}
		});
		buttonFactory.attachClickCursor(exitButton, GameCursor.SELECT);
		table.add(exitButton).expandX().align(Align.topLeft).padLeft(leftPadding).padTop(5f).row();
		table.add().grow();
		return table;
	}

	private Actor buildPaperComponents() {
		Label titleLabel = labelFactory.titleRibbon("GUI.SETTLER_MANAGEMENT.TITLE");

		Table populationRow = new Table();
		populationStatisticsLabel = new Label("", managementSkin, "sort_by_label");
		populationRow.add(populationStatisticsLabel);
		rebuildPopulationStatistics();

		ImageTextButton immigrationToggle = widgetFactory.createLeftLabelledToggle("GUI.SETTLER_MANAGEMENT.IMMIGRATION", managementSkin, null);
		if (gameContext != null) {
			immigrationToggle.setChecked(gameContext.getSettlementState().isAllowImmigration());
		}
		immigrationToggle.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				gameContext.getSettlementState().setAllowImmigration(immigrationToggle.isChecked());
			}
		});
		populationRow.add(immigrationToggle).padTop(66f).padBottom(66f).padLeft(150f).spaceRight(38);


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
				filterAndRebuildSettlerTable();
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
		table.add(titleLabel).padTop(54).row();
		table.add(professionButtons).padTop(80).row();
		table.add(populationRow).right().row();
		table.add(filters).left().row();
		table.add(new Image(managementSkin.getDrawable("asset_line"))).height(12f).padTop(40f).row();
		table.add(scrollPane).height(1300).grow().row();
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
					filterAndRebuildSettlerTable();
				}
			}
		});
		tooltipFactory.simpleTooltip(button, i18nKey, TooltipLocationHint.ABOVE);
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
				filterAndRebuildSettlerTable();
			}
		});
		buttonFactory.attachClickCursor(button, GameCursor.SELECT);
		return button;
	}

	private void filterAndRebuildSettlerTable() {
		Collection<Entity> settlers = settlerTracker.getLiving()
				.stream()
				.filter(selectedFilter)
				.filter(settler -> getName(settler).toLowerCase().contains(searchBarText.toLowerCase()))
				.sorted(selectedSortFunction)
				.collect(Collectors.toList());

		rebuildSettlerTable(settlers);
	}

	private void rebuildSettlerTable(Collection<Entity> settlers) {
		filterCountLabel.setText(settlers.size());

		Table settlersTable = new Table();
		settlersTable.align(Align.top);

		for (Entity settler : settlers) {
			boolean isMilitary = IS_MILITARY.test(settler);
			Consumer<Entity> rebuildSettlerView = me -> {
				rebuildPopulationStatistics();
				rebuildSettlerTable(settlers);
			};
			Table mugshotColumn = mugshot(settler);
			Table textSummaryColumn = textSummary(settler);
			Table happinessColumn = happiness(settler);
			Table needsColumn = needs(settler);
			Table professionsColumn = professions(settler, rebuildSettlerView);
			Table weaponSelectColumn = weaponSelection(settler);
			Table militaryToggleColumn = militaryToggle(settler, rebuildSettlerView);

			addGotoSettlerBehaviour(mugshotColumn, settler);


			settlersTable.add(mugshotColumn).spaceRight(50f);
			settlersTable.add(textSummaryColumn).fillX().spaceRight(50f);
			if (isMilitary) {
				settlersTable.add(new Table()).growX().spaceRight(50f);
				settlersTable.add(needsColumn).growX().spaceRight(50f);
				settlersTable.add(weaponSelectColumn).growX().spaceRight(50f).spaceBottom(76f).spaceTop(38f);
			} else {
				settlersTable.add(happinessColumn).growX().spaceRight(50f);
				settlersTable.add(needsColumn).growX().spaceRight(50f);
				settlersTable.add(professionsColumn).growX().spaceRight(50f).spaceBottom(76f).spaceTop(38f);
			}

			settlersTable.add(militaryToggleColumn).growX().spaceRight(36f);

			settlersTable.left().row();
		}

		scrollPane.setActor(settlersTable);
	}

	private Table weaponSelection(Entity settler) {
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);

		Table table = new Table();

		if (skillsComponent != null) {

			Skill weaponSkill = skillDictionary.getByName("SWORDSMANSHIP");//this comes from the WeaponInfo of an item or if no weapon, SkillsDictionary.UNARMED_COMBAT_SKILL

			//TODO: need main hand and off hand enabling/disabling
			Table weaponColumn = new Table();
			Image weaponIcon = new Image(managementSkin.getDrawable("icon_military_equip_weapon"));
			ImageButton.ImageButtonStyle weaponStyle = new ImageButton.ImageButtonStyle(managementSkin.get("military_equipment_assignment", ImageButton.ImageButtonStyle.class));
			ImageButton weaponSelectButton = new ImageButton(weaponStyle);
			buttonFactory.attachClickCursor(weaponSelectButton, GameCursor.SELECT);
			Table weaponProgress = settlerProfessionFactory.buildProgressBarRow(skillsComponent, weaponSkill, false);
			weaponColumn.add(weaponIcon).row();
			weaponColumn.add(weaponSelectButton).spaceTop(10f).spaceBottom(6f).row();
			weaponColumn.add(weaponProgress);
			table.add(weaponColumn).growX().top().spaceRight(24).spaceLeft(24); //todo fix the position when switching between military and civilian


			//todo: if two handed weapon, make semi-transparent and disabled
			Table shieldColumn = new Table();
			Image shieldIcon = new Image(managementSkin.getDrawable("icon_military_equip_shield"));
			ImageButton.ImageButtonStyle shieldStyle = new ImageButton.ImageButtonStyle(managementSkin.get("military_equipment_assignment", ImageButton.ImageButtonStyle.class));
			ImageButton shieldSelectButton = new ImageButton(shieldStyle);
			buttonFactory.attachClickCursor(shieldSelectButton, GameCursor.SELECT);
			shieldColumn.add(shieldIcon).expandX().row();
			shieldColumn.add(shieldSelectButton).spaceTop(10f).spaceBottom(6f).row();
			table.add(shieldColumn).growX().top().spaceRight(24).spaceLeft(24);

			Table armourColumn = new Table();
			Image armourIcon = new Image(managementSkin.getDrawable("icon_military_equip_armour"));
			ImageButton.ImageButtonStyle armourStyle = new ImageButton.ImageButtonStyle(managementSkin.get("military_equipment_assignment", ImageButton.ImageButtonStyle.class));
			ImageButton armourSelectButton = new ImageButton(armourStyle);
			buttonFactory.attachClickCursor(armourSelectButton, GameCursor.SELECT);
			armourColumn.add(armourIcon).expandX().row();
			armourColumn.add(armourSelectButton).spaceTop(10f).spaceBottom(6f).row();
			table.add(armourColumn).growX().top().spaceRight(24).spaceLeft(24);

		}




		return table;
	}

	private Table militaryToggle(Entity settler, Consumer<Entity> onMilitaryChange) {
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);

		Image image = new Image(managementSkin.getDrawable("icon_military"));
		ImageTextButton toggle = widgetFactory.createLeftLabelledToggle("GUI.SETTLER_MANAGEMENT.PROFESSION.MILITARY", managementSkin, image);
		toggle.setChecked(IS_MILITARY.test(settler));
		toggle.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				boolean checked = toggle.isChecked();
				if (checked) {
					militaryComponent.addToMilitary(1L);
				} else {
					militaryComponent.removeFromMilitary();
				}
				onMilitaryChange.accept(settler);
			}
		});

		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
		String militaryProficiencyText = "";
		if (skillsComponent != null) {
			militaryProficiencyText = getMilitaryProficiencyText(settler, skillsComponent);
		}

		Label militaryProficiencyLabel = new Label(militaryProficiencyText, managementSkin, "military_highest_proficiency_label");
		militaryProficiencyLabel.setAlignment(Align.center);

		Table table = new Table();
		table.add(toggle).row();
		table.add(militaryProficiencyLabel);
		return table;
	}

	private Table professions(Entity settler, Consumer<Entity> rebuildSettlerView) {
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
		if (skillsComponent == null) {
			return new Table();
		}

		Table table = new Table();
		settlerProfessionFactory.addProfessionComponents(settler, table, rebuildSettlerView);
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
					Image icon = new Image(managementSkin.getDrawable(need.iconName()));
					tooltipFactory.simpleTooltip(icon, need.getI18nKey(), TooltipLocationHint.BELOW);
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


					table.add(icon).right().spaceRight(28);
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
			StringBuilder modifierBuilder = new StringBuilder();
			modifierBuilder.append(" ");
			final String drawableName;
			if (modifierAmount > 0) {
				drawableName = "icon_happy";
				modifierBuilder.append("+");
			} else {
				drawableName = "icon_sad";
			}
			Image modifierImage = new Image(managementSkin.getDrawable(drawableName));

			modifierBuilder.append(modifierAmount);

			I18nText happinessModifierText = i18nTranslator.getTranslatedString(modifier.getI18nKey());
			happinessModifierText.append(new I18nWord(modifierBuilder.toString()));

			tooltipFactory.simpleTooltip(modifierImage, happinessModifierText, TooltipLocationHint.BELOW);

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
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);

		String settlerName = getName(settler);
		String currentProfessionName = "";
		//TODO: if in military, get rank
		if (skillsComponent != null) {
			if (IS_MILITARY.test(settler)) {
				currentProfessionName = getMilitaryProficiencyText(settler, skillsComponent);
			} else {
				java.util.List<SkillsComponent.QuantifiedSkill> activeProfessions = skillsComponent.getActiveProfessions();
				if (!activeProfessions.isEmpty()) {
					SkillsComponent.QuantifiedSkill quantifiedSkill = activeProfessions.get(0);
					currentProfessionName = i18nTranslator.getSkilledProfessionDescription(quantifiedSkill.getSkill(), quantifiedSkill.getLevel(),
							((CreatureEntityAttributes) settler.getPhysicalEntityComponent().getAttributes()).getGender()).toString();
				}
			}
		}

		java.util.List<String> behaviourDescriptions = new ArrayList<>();
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

		Label professionLabel = tableLabel(currentProfessionName);
		professionLabel.setAlignment(Align.left);
		table.add(professionLabel).growX().row();

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


	private String getMilitaryProficiencyText(Entity settler, SkillsComponent skillsComponent) {
		String militaryProficiencyText;
		Skill highestSkill = SkillDictionary.UNARMED_COMBAT_SKILL;
		int highestSkillLevel = skillsComponent.getSkillLevel(highestSkill);
		for (Skill combatSkill : skillDictionary.getAllCombatSkills()) {
			int combatSkillLevel = skillsComponent.getSkillLevel(combatSkill);
			if (combatSkillLevel > highestSkillLevel) {
				highestSkill = combatSkill;
				highestSkillLevel = combatSkillLevel;
			}
		}

		militaryProficiencyText = i18nTranslator.getSkilledProfessionDescription(highestSkill, highestSkillLevel,
				((CreatureEntityAttributes) settler.getPhysicalEntityComponent().getAttributes()).getGender()).toString();
		return militaryProficiencyText;
	}


	private void rebuildPopulationStatistics() {
		int civilianCount = 0;
		int civilianBedCount = 0;
		int militaryCount = 0;
		int militaryBedCount = 0;

		for (Entity bed : settlementFurnitureTracker.findByTag(BedSleepingPositionTag.class, false)) {
			SleepingPositionComponent sleepingComponent = bed.getComponent(SleepingPositionComponent.class);
			if (sleepingComponent != null) {
				if (sleepingComponent.getAssignmentType() == BedSleepingPositionTag.BedAssignment.MILITARY_ONLY) {
					militaryBedCount++;
				} else if (sleepingComponent.getAssignmentType() == BedSleepingPositionTag.BedAssignment.CIVILIAN_ONLY) {
					civilianBedCount++;
				}
			}
		}
		Collection<Entity> livingSettlers = settlerTracker.getLiving();
		for (Entity livingSettler : livingSettlers) {
			if (IS_MILITARY.test(livingSettler)) {
				militaryCount++;
			} else if (IS_CIVILIAN.test(livingSettler)) {
				civilianCount++;
			}
		}
		I18nText populationStatisticsText = i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.POPULATION_STATISTICS", Map.of(
				"civilianCount", new I18nWord(String.valueOf(civilianCount)),
				"civilianBedCount", new I18nWord(String.valueOf(civilianBedCount)),
				"militaryCount", new I18nWord(String.valueOf(militaryCount)),
				"militaryBedCount", new I18nWord(String.valueOf(militaryBedCount)),
				"populationCount", new I18nWord(String.valueOf(livingSettlers.size()))
		));
		populationStatisticsLabel.setText(populationStatisticsText.toString());
	}


	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

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

	private Label tableLabel(String text) {
		return new Label(text, managementSkin, "table_value_label");
	}
}
