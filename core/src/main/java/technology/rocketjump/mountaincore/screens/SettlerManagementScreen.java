package technology.rocketjump.mountaincore.screens;

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
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import technology.rocketjump.mountaincore.assets.entities.tags.BedSleepingPositionTag;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.ai.goap.EntityNeed;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.components.creature.*;
import technology.rocketjump.mountaincore.entities.components.furniture.SleepingPositionComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.mountaincore.entities.model.physical.combat.DefenseType;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Sanity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.LossOfMainHand;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.LossOfOffHand;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemQuality;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundMessage;
import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.entities.EntityRenderer;
import technology.rocketjump.mountaincore.rendering.utils.ColorMixer;
import technology.rocketjump.mountaincore.settlement.SettlementFurnitureTracker;
import technology.rocketjump.mountaincore.settlement.SettlementItemTracker;
import technology.rocketjump.mountaincore.settlement.SettlerTracker;
import technology.rocketjump.mountaincore.ui.Selectable;
import technology.rocketjump.mountaincore.ui.Updatable;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.mountaincore.ui.i18n.*;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.skins.ManagementSkin;
import technology.rocketjump.mountaincore.ui.skins.MenuSkin;
import technology.rocketjump.mountaincore.ui.widgets.*;
import technology.rocketjump.mountaincore.ui.widgets.libgdxclone.MountaincoreTextField;

import javax.inject.Singleton;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class SettlerManagementScreen extends AbstractGameScreen implements DisplaysText, GameContextAware {
	public static final Predicate<Entity> IS_MILITARY = settler -> {
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
		return militaryComponent != null && militaryComponent.isInMilitary();
	};
	public static final Predicate<Entity> IS_CIVILIAN = IS_MILITARY.negate();
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

	private static final Comparator<Entity> SORT_HAPPINESS = Comparator.comparingInt(settler -> {
		if (IS_MILITARY.test(settler)) {
			return 0;
		} else {
			return settler.getComponent(HappinessComponent.class).getNetModifier();
		}
	});
	public static final Comparator<Entity> SORT_NAME = Comparator.comparing(SettlerManagementScreen::getName);
	public static final Comparator<Entity> SORT_MILITARY_CIVILIAN = Comparator.comparing((Function<Entity, Long>) settler -> {
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
	private final SettlementItemTracker settlementItemTracker;
	private final SoundAssetDictionary soundAssetDictionary;
	private final SoundAsset conscriptSoundAsset;


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
								   SettlementFurnitureTracker settlementFurnitureTracker, SettlementItemTracker settlementItemTracker,
								   SoundAssetDictionary soundAssetDictionary, UserPreferences userPreferences) {
		super(userPreferences, messageDispatcher);
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
		this.settlementItemTracker = settlementItemTracker;
		this.soundAssetDictionary = soundAssetDictionary;
		this.conscriptSoundAsset = soundAssetDictionary.getByName("ConfirmConscript");
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
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollBarPositions(true, true);

		stack = new Stack();
		stack.setFillParent(true);
		stack.add(menuSkin.buildBackgroundBaseLayer());
		stack.add(menuSkin.buildPaperLayer(buildPaperComponents(), 136, true, false));
		stack.add(buildExitTable(136 + menuSkin.getDrawable("paper_texture_bg_pattern_thin").getMinWidth() + 5f));

		stage.addActor(stack);
	}


	private Actor buildExitTable(float rightPadding) {
		Table table = new Table();
		Button exitButton = new Button(menuSkin, "btn_exit");
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
			}
		});
		buttonFactory.attachClickCursor(exitButton, GameCursor.SELECT);
		table.add(exitButton).expandX().align(Align.topRight).padRight(rightPadding).padTop(5f).row();
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
		for (Skill profession : skillDictionary.getSelectableProfessions()) {
			professionFilterButton(professionButtonGroup, professionButtons, profession.getIcon(), profession.getI18nKey(), new MatchesActiveProfession(profession));
		}
		professionFilterButton(professionButtonGroup, professionButtons, "settlers_job_villager", "GUI.SETTLER_MANAGEMENT.PROFESSION.VILLAGER", new MatchesActiveProfession(SkillDictionary.NULL_PROFESSION));

		TextField searchBar = new MountaincoreTextField("", managementSkin, "search_bar_input");
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
		Label sortByLabel = new Label(i18nTranslator.translate("GUI.SETTLER_MANAGEMENT.SORT_BY"), managementSkin, "sort_by_label");
		Button sortByHappiness = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.HAPPINESS", SORT_HAPPINESS);
		Button sortByName = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.NAME", SORT_NAME);
		Button sortBySkillLevel = buildTextSortButton("GUI.SETTLER_MANAGEMENT.SORT.SKILL_LEVEL", Comparator.comparing((Function<Entity, Float>) settler -> {
			SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
			if (skillsComponent == null) {
				return 0f;
			}
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
		table.add(scrollPane).grow().row();
		return table;
	}

	private void professionFilterButton(ButtonGroup<ImageButton> professionButtonGroup, Table professionButtons, String drawableName, String i18nKey, Predicate<Entity> filter) {
		Drawable drawable = managementSkin.getDrawable(drawableName);
		ImageButton button = buttonFactory.checkableButton(drawable, false);
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
				.filter(settler -> getName(settler).toLowerCase(Locale.ROOT).contains(searchBarText.toLowerCase(Locale.ROOT)))
				.sorted(selectedSortFunction.thenComparing(SORT_NAME))
				.collect(Collectors.toList());

		rebuildSettlerTable(settlers);
	}

	private void rebuildSettlerTable(Collection<Entity> settlers) {
		filterCountLabel.setText(settlers.size());

		Table settlersTable = new Table();
		settlersTable.align(Align.top);
		settlersTable.padTop(38);
		settlersTable.padBottom(38);

		for (Entity settler : settlers) {
			boolean isMilitary = IS_MILITARY.test(settler);
			Consumer<Entity> rebuildSettlerView = me -> {
				rebuildPopulationStatistics();
				rebuildSettlerTable(settlers);
			};
			Table mugshotColumn = mugshot(settler);
			Table textSummaryColumn = textSummary(settler);
			Table happinessColumn = happiness(settler).getActor();
			Table needsColumn = needs(settler).getActor();
			Table professionsColumn = professions(settler, 1f, rebuildSettlerView);
			Table weaponSelectColumn = weaponSelection(settler, 1.0f, rebuildSettlerView).getActor();
			Table militaryToggleColumn = militaryToggle(settler, true, rebuildSettlerView).getActor();
			Table squadColumn = militarySquad(settler);

			addGotoSettlerBehaviour(mugshotColumn, settler);


			settlersTable.add(mugshotColumn).spaceRight(50f);
			settlersTable.add(textSummaryColumn).width(550).spaceRight(50f);
			if (isMilitary) {
				settlersTable.add(squadColumn).width(500).spaceRight(50f);
				settlersTable.add(needsColumn).spaceRight(50f);
				settlersTable.add(weaponSelectColumn).spaceRight(50f).spaceBottom(76f).spaceTop(38f);
			} else {
				settlersTable.add(happinessColumn).width(500).spaceRight(50f);
				settlersTable.add(needsColumn).spaceRight(50f);
				settlersTable.add(professionsColumn).spaceRight(50f).spaceBottom(76f).spaceTop(38f);
			}

			settlersTable.add(militaryToggleColumn).growX().spaceRight(36f);

			settlersTable.left().row();
		}

		scrollPane.setActor(settlersTable);
	}

	public Updatable<Table> weaponSelection(Entity settler, float overallScale, Consumer<Entity> onSettlerChange) {
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
		EquippedItemComponent equippedItemComponent = settler.getComponent(EquippedItemComponent.class);

		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);

		if (skillsComponent != null && militaryComponent != null) {
			Drawable notEquippedIcon = managementSkin.getDrawable("icon_not_equipped");
			Drawable brawlIcon = managementSkin.getDrawable("icon_brawl");

			float scaleFactor = 0.9f * overallScale;
			ImageButton.ImageButtonStyle weaponButtonStyle = new ImageButton.ImageButtonStyle(managementSkin.get("military_equipment_assignment", ImageButton.ImageButtonStyle.class));
			ImageButton.ImageButtonStyle shieldButtonStyle = new ImageButton.ImageButtonStyle(managementSkin.get("military_equipment_assignment", ImageButton.ImageButtonStyle.class));
			ImageButton.ImageButtonStyle armourButtonStyle = new ImageButton.ImageButtonStyle(managementSkin.get("military_equipment_assignment", ImageButton.ImageButtonStyle.class));
			updatable.regularly(() -> {
				if (gameContext.getEntity(militaryComponent.getAssignedWeaponId()) == null) {
					weaponButtonStyle.imageUp = brawlIcon;
				} else {
					EntityDrawable weaponDrawable = new EntityDrawable(gameContext.getEntity(militaryComponent.getAssignedWeaponId()), entityRenderer, true, messageDispatcher);
					weaponDrawable.setMinSize(weaponButtonStyle.up.getMinWidth() * scaleFactor, weaponButtonStyle.up.getMinHeight() * scaleFactor);
					weaponButtonStyle.imageUp = weaponDrawable;
				}
			});
			updatable.regularly(() -> {
				if (gameContext.getEntity(militaryComponent.getAssignedShieldId()) == null) {
					shieldButtonStyle.imageUp = notEquippedIcon;
				} else {
					EntityDrawable shieldDrawable = new EntityDrawable(gameContext.getEntity(militaryComponent.getAssignedShieldId()), entityRenderer, true, messageDispatcher);
					shieldDrawable.setMinSize(shieldButtonStyle.up.getMinWidth() * scaleFactor, shieldButtonStyle.up.getMinHeight() * scaleFactor);
					shieldButtonStyle.imageUp = shieldDrawable;
				}
			});
			updatable.regularly(() -> {
				if (gameContext.getEntity(militaryComponent.getAssignedArmorId()) == null) {
					armourButtonStyle.imageUp = notEquippedIcon;
				} else {
					EntityDrawable armourDrawable = new EntityDrawable(gameContext.getEntity(militaryComponent.getAssignedArmorId()), entityRenderer, true, messageDispatcher);
					armourDrawable.setMinSize(armourButtonStyle.up.getMinWidth() * scaleFactor, armourButtonStyle.up.getMinHeight() * scaleFactor);
					armourButtonStyle.imageUp = armourDrawable;
				}
			});

			Entity assignedWeapon = gameContext.getEntity(militaryComponent.getAssignedWeaponId());
			Entity assignedShield = gameContext.getEntity(militaryComponent.getAssignedShieldId());
			Entity assignedArmour = gameContext.getEntity(militaryComponent.getAssignedArmorId());
			Skill weaponSkill = SkillDictionary.UNARMED_COMBAT_SKILL;
			boolean weaponIsTwoHanded = false;
			if (assignedWeapon == null) {
				militaryComponent.setAssignedWeaponId(null);
			} else {
				WeaponInfo weaponInfo = getWeaponInfo(assignedWeapon);
				if (weaponInfo != null) {
					weaponIsTwoHanded = weaponInfo.isTwoHanded();
					weaponSkill = weaponInfo.getCombatSkill();
				}
			}
			if (assignedShield == null) {
				militaryComponent.setAssignedShieldId(null);
			}
			if (assignedArmour == null) {
				militaryComponent.setAssignedArmorId(null);
			}


			Table weaponColumn = new Table();
			Image weaponIcon = new Image(managementSkin.getDrawable("icon_military_equip_weapon"));
			ImageButton weaponSelectButton = new ImageButton(weaponButtonStyle) {
				@Override
				public float getPrefWidth() {
					return super.getPrefWidth() * overallScale;
				}

				@Override
				public float getPrefHeight() {
					return super.getPrefHeight() * overallScale;
				}
			};

			buttonFactory.attachClickCursor(weaponSelectButton, GameCursor.SELECT);
			Table weaponProgress = settlerProfessionFactory.buildProgressBarRow(skillsComponent, weaponSkill, false);
			weaponColumn.add(weaponIcon).row();
			weaponColumn.add(weaponSelectButton).spaceTop(10f).spaceBottom(6f).row();
			weaponColumn.add(weaponProgress);

			Table shieldColumn = new Table();
			Image shieldIcon = new Image(managementSkin.getDrawable("icon_military_equip_shield"));
			ImageButton shieldSelectButton = new ImageButton(shieldButtonStyle) {
				@Override
				public float getPrefWidth() {
					return super.getPrefWidth() * overallScale;
				}

				@Override
				public float getPrefHeight() {
					return super.getPrefHeight() * overallScale;
				}
			};
			buttonFactory.attachClickCursor(shieldSelectButton, GameCursor.SELECT);
			shieldColumn.add(shieldIcon).expandX().row();
			shieldColumn.add(shieldSelectButton).spaceTop(10f).spaceBottom(6f).row();

			Table armourColumn = new Table();
			Image armourIcon = new Image(managementSkin.getDrawable("icon_military_equip_armour"));
			ImageButton armourSelectButton = new ImageButton(armourButtonStyle) {
				@Override
				public float getPrefWidth() {
					return super.getPrefWidth() * overallScale;
				}

				@Override
				public float getPrefHeight() {
					return super.getPrefHeight() * overallScale;
				}
			};
			buttonFactory.attachClickCursor(armourSelectButton, GameCursor.SELECT);
			armourColumn.add(armourIcon).expandX().row();
			armourColumn.add(armourSelectButton).spaceTop(10f).spaceBottom(6f).row();


			if (assignedWeapon != null) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) assignedWeapon.getPhysicalEntityComponent().getAttributes();
				I18nText tooltipText = i18nTranslator.getItemDescription(1, attributes.getPrimaryMaterial(), attributes.getItemType(), attributes.getItemQuality());
				tooltipFactory.simpleTooltip(weaponSelectButton, tooltipText, TooltipLocationHint.BELOW);
			} else {
				tooltipFactory.simpleTooltip(weaponSelectButton, "WEAPON.UNARMED", TooltipLocationHint.BELOW);
			}

			if (assignedShield != null) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) assignedShield.getPhysicalEntityComponent().getAttributes();
				I18nText tooltipText = i18nTranslator.getItemDescription(1, attributes.getPrimaryMaterial(), attributes.getItemType(), attributes.getItemQuality());
				tooltipFactory.simpleTooltip(shieldSelectButton, tooltipText, TooltipLocationHint.BELOW);
			} else {
				tooltipFactory.simpleTooltip(shieldSelectButton, "WEAPON.NO_SHIELD", TooltipLocationHint.BELOW);
			}

			if (assignedArmour != null) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) assignedArmour.getPhysicalEntityComponent().getAttributes();
				I18nText tooltipText = i18nTranslator.getItemDescription(1, attributes.getPrimaryMaterial(), attributes.getItemType(), attributes.getItemQuality());
				tooltipFactory.simpleTooltip(armourSelectButton, tooltipText, TooltipLocationHint.BELOW);
			} else {
				tooltipFactory.simpleTooltip(armourSelectButton, "WEAPON.NO_ARMOUR", TooltipLocationHint.BELOW);
			}

			Consumer<Entity> updateState = entity -> {
				militaryComponent.infrequentUpdate(0.0);
				onSettlerChange.accept(settler);
			};


			boolean canUseMainHand = equippedItemComponent == null || equippedItemComponent.isMainHandEnabled();
			boolean canUseOffHand = equippedItemComponent == null || equippedItemComponent.isOffHandEnabled();
			boolean canUseWeapon = canUseMainHand;
			boolean canUseShield = !weaponIsTwoHanded && canUseOffHand;

			weaponSelectButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					super.clicked(event, x, y);
					//TODO: refactor out common select dialog wizard
					Drawable brawlDrawable = managementSkin.getDrawable("military_icon_select_brawl");

					Map<String, List<Entity>> weaponsByItemType = settlementItemTracker.getAll(true).stream()
							.filter(e -> getWeaponInfo(e) != null)
							.filter(e -> !getWeaponInfo(e).isTwoHanded() || canUseOffHand)
							.collect(Collectors.groupingBy(SettlementItemTracker.GROUP_BY_ITEM_TYPE));
					List<SelectItemDialog.Option> options = new ArrayList<>();
					weaponsByItemType.keySet().stream().sorted().forEach(weaponName -> {
						List<Entity> subGroup = weaponsByItemType.get(weaponName);
						Entity exampleWeapon = subGroup.get(0);
						EntityDrawable entityDrawable = new EntityDrawable(exampleWeapon, entityRenderer, true, messageDispatcher);
						entityDrawable.setMinSize(brawlDrawable.getMinWidth(), brawlDrawable.getMinHeight());
						ItemEntityAttributes attributes = (ItemEntityAttributes) exampleWeapon.getPhysicalEntityComponent().getAttributes();
						options.add(new SelectWeaponTypeOption(attributes.getItemType().getI18nKey(), entityDrawable, skillsComponent, getWeaponInfo(exampleWeapon).getCombatSkill(), subGroup, weapon -> {
							militaryComponent.setAssignedWeaponId(weapon.getId());
							updateState.accept(weapon);
						}));
					});

					//TODO: feels dirty but deals with the inconsistency of unarmed being mixed in with groups
					options.add(new SelectWeaponTypeOption("WEAPON.UNARMED", brawlDrawable, skillsComponent, SkillDictionary.UNARMED_COMBAT_SKILL, Collections.emptyList(), null) {
						@Override
						public void onSelect() {
							militaryComponent.setAssignedWeaponId(null);
							updateState.accept(null);
						}
					});

					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, new SelectItemDialog(i18nTranslator.getTranslatedString("GUI.SETTLER_MANAGEMENT.CHOOSE_WEAPON"),
							menuSkin, messageDispatcher, soundAssetDictionary, tooltipFactory, options, 6));
				}
			});

			shieldSelectButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					super.clicked(event, x, y);
					List<Entity> allShields = settlementItemTracker.getAll(true).stream()
							.filter(e -> getDefenseInfo(e) != null
									&& DefenseType.SHIELD.equals(getDefenseInfo(e).getType()))
							.toList();

					List<SelectItemDialog.Option> options = SelectItemOption.forMaterialAndQuality(allShields, entityRenderer, messageDispatcher, i18nTranslator, shield -> {
						militaryComponent.setAssignedShieldId(shield.getId());
						updateState.accept(shield);
					}, managementSkin);
					options.add(new SelectItemOption(i18nTranslator.getTranslatedString("WEAPON.NO_SHIELD"), null, managementSkin.getDrawable("military_icon_select_clear"), n -> {
						militaryComponent.setAssignedShieldId(null);
						updateState.accept(n);
					}, managementSkin));

					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, new SelectItemDialog(i18nTranslator.getTranslatedString("GUI.SETTLER_MANAGEMENT.CHOOSE_SHIELD"),
							menuSkin, messageDispatcher, soundAssetDictionary, tooltipFactory, options, 6, "EquipShield"));
				}
			});

			armourSelectButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					super.clicked(event, x, y);

					List<Entity> allArmour = settlementItemTracker.getAll(true).stream()
							.filter(e -> getDefenseInfo(e) != null
									&& DefenseType.ARMOR.equals(getDefenseInfo(e).getType())
									&& getDefenseInfo(e).canBeEquippedBy(settler)
							)
							.toList();

					List<SelectItemDialog.Option> options = SelectItemOption.forMaterialAndQuality(allArmour, entityRenderer, messageDispatcher, i18nTranslator, armour -> {
						militaryComponent.setAssignedArmorId(armour.getId());
						updateState.accept(armour);
					}, managementSkin);
					options.add(new SelectItemOption(i18nTranslator.getTranslatedString("WEAPON.NO_ARMOUR"), null, managementSkin.getDrawable("military_icon_select_clear"), n -> {
						militaryComponent.setAssignedArmorId(null);
						updateState.accept(n);
					}, managementSkin));

					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, new SelectItemDialog(i18nTranslator.getTranslatedString("GUI.SETTLER_MANAGEMENT.CHOOSE_ARMOUR"),
							menuSkin, messageDispatcher, soundAssetDictionary, tooltipFactory, options, 6, "EquipArmor"));
				}
			});

			Stack weaponStack = new Stack();
			weaponStack.add(weaponColumn);
			Stack shieldStack = new Stack();
			shieldStack.add(shieldColumn);

			if (canUseWeapon) {
				enable(weaponColumn);
			} else {
				disable(weaponColumn);
				final I18nText disableReason;

				//TODO expand me in future or reverse lookup
				I18nString statusReason = I18nWord.BLANK;
				if (settler.getComponent(StatusComponent.class) != null && settler.getComponent(StatusComponent.class).contains(LossOfMainHand.class)) {
					statusReason = i18nTranslator.getTranslatedString(new LossOfMainHand().getI18Key());
				}
				disableReason = i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.MAIN_HAND.STATUS",
						Map.of("status", statusReason));

				//ugly but works
				Image invisibleOverlay = new Image(managementSkin.getDrawable("invisible_pixel"));
				invisibleOverlay.setWidth(shieldColumn.getMinWidth());
				invisibleOverlay.setHeight(shieldColumn.getMinHeight());
				weaponStack.add(invisibleOverlay);
				tooltipFactory.simpleTooltip(invisibleOverlay, disableReason, TooltipLocationHint.BELOW);
			}

			if (canUseShield) {
				enable(shieldColumn);
			} else {
				disable(shieldColumn);
				final I18nText disableReason;
				if (weaponIsTwoHanded) {
					ItemEntityAttributes itemEntityAttributes = (ItemEntityAttributes) assignedWeapon.getPhysicalEntityComponent().getAttributes();
					disableReason = i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.OFF_HAND.TWO_HANDED_WEAPON",
							Map.of("weaponName", i18nTranslator.getTranslatedString(itemEntityAttributes.getItemType().getI18nKey())));
				} else {
					//TODO expand me in future or reverse lookup
					I18nString statusReason = I18nWord.BLANK;
					if (settler.getComponent(StatusComponent.class) != null && settler.getComponent(StatusComponent.class).contains(LossOfOffHand.class)) {
						statusReason = i18nTranslator.getTranslatedString(new LossOfOffHand().getI18Key());
					}
					disableReason = i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.OFF_HAND.STATUS",
							Map.of("status", statusReason));
				}
				//ugly but works
				Image invisibleOverlay = new Image(managementSkin.getDrawable("invisible_pixel"));
				invisibleOverlay.setWidth(shieldColumn.getMinWidth());
				invisibleOverlay.setHeight(shieldColumn.getMinHeight());
				shieldStack.add(invisibleOverlay);
				tooltipFactory.simpleTooltip(invisibleOverlay, disableReason, TooltipLocationHint.BELOW);
			}

			table.add(weaponStack).growX().top().spaceRight(24 * overallScale).spaceLeft(24 * overallScale);
			table.add(shieldStack).growX().top().spaceRight(24 * overallScale).spaceLeft(24 * overallScale);
			table.add(armourColumn).growX().top().spaceRight(24 * overallScale).spaceLeft(24 * overallScale);
		}
		updatable.update();

		return updatable;
	}

	static class SelectItemOption extends SelectItemDialog.Option {

		private final ManagementSkin managementSkin;
		private final Entity item;
		private final Drawable drawable;
		private final Consumer<Entity> onSelect;
		private final int quantity;
		private final ItemQuality itemQuality;

		public static List<SelectItemDialog.Option> forMaterialAndQuality(List<Entity> entities, EntityRenderer entityRenderer,
																		  MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
																		  Consumer<Entity> onSelect, ManagementSkin managementSkin) {
			Map<String, List<Entity>> byMaterialAndQuality = entities.stream().collect(Collectors.groupingBy(SettlementItemTracker.GROUP_BY_ITEM_TYPE_MATERIAL_AND_QUALITY));

			List<SelectItemDialog.Option> options = new ArrayList<>();
			byMaterialAndQuality.keySet().stream().sorted().forEach(key -> {
				List<Entity> subGroup = byMaterialAndQuality.get(key);
				Entity exampleEntity = subGroup.get(0);

				EntityDrawable entityDrawable = new EntityDrawable(exampleEntity, entityRenderer, true, messageDispatcher);
				entityDrawable.setMinSize(206, 206); //todo: fix me
				ItemEntityAttributes attributes = (ItemEntityAttributes) exampleEntity.getPhysicalEntityComponent().getAttributes();
				I18nText tooltipText = i18nTranslator.getItemDescription(1, attributes.getPrimaryMaterial(), attributes.getItemType(), attributes.getItemQuality());
				options.add(new SelectItemOption(tooltipText, exampleEntity, entityDrawable, onSelect, managementSkin, subGroup.size(), attributes.getItemQuality()));
			});

			return options;
		}

		public SelectItemOption(I18nText tooltipText, Entity item, Drawable drawable, Consumer<Entity> onSelect, ManagementSkin managementSkin) {
			this(tooltipText, item, drawable, onSelect, managementSkin, 0, null);
		}

		public SelectItemOption(I18nText tooltipText, Entity item, Drawable drawable, Consumer<Entity> onSelect, ManagementSkin managementSkin, int quantity, ItemQuality itemQuality) {
			super(tooltipText);
			this.item = item;
			this.drawable = drawable;
			this.onSelect = onSelect;
			this.quantity = quantity;
			this.itemQuality = itemQuality;
			this.managementSkin = managementSkin;
		}

		@Override
		public void addSelectionComponents(Table innerTable) {
			Image image = new Image(drawable);

			Stack entityStack = new Stack();
			entityStack.add(image);

			if (quantity > 1) {
				Label amountLabel = new Label(String.valueOf(quantity), managementSkin, "entity_drawable_quantity_label");
				amountLabel.setAlignment(Align.center);
				Table amountTable = new Table();
				amountTable.add(amountLabel).left().top();
				amountTable.add(new Container<>()).width(image.getMinWidth() - 32f).expandX().row();
				amountTable.add(new Container<>()).colspan(2).height(image.getMinHeight() - 32f).expandY();
				entityStack.add(amountTable);
			}

			if (itemQuality != null) {
				Table qualityTable = new Table();
				qualityTable.add(new Container<>()).colspan(2).expandY().row();
				qualityTable.add(new Container<>()).expandX();
				qualityTable.add(new Image(managementSkin.getQualityDrawableForCorner(itemQuality))).right().bottom();
				entityStack.add(qualityTable);
			}
			innerTable.add(entityStack).pad(10);
		}

		@Override
		public void onSelect() {
			onSelect.accept(item);
		}
	}

	class SelectWeaponTypeOption extends SelectItemDialog.Option {

		private final Drawable drawable;
		private final SkillsComponent skillsComponent;
		private final Skill skill;
		private final List<Entity> subGroup;
		private final Consumer<Entity> onWeaponSelect;

		public SelectWeaponTypeOption(String tooltipI18nKey, Drawable drawable, SkillsComponent skillsComponent, Skill skill, List<Entity> subGroup, Consumer<Entity> onWeaponSelect) {
			super(i18nTranslator.getTranslatedString(tooltipI18nKey));
			this.drawable = drawable;
			this.skillsComponent = skillsComponent;
			this.skill = skill;
			this.subGroup = subGroup;
			this.onWeaponSelect = onWeaponSelect;
		}

		@Override
		public void addSelectionComponents(Table innerTable) {
			Image image = new Image(drawable);
			innerTable.add(image).pad(10).row();
			innerTable.add(settlerProfessionFactory.buildProgressBarRow(skillsComponent, skill, true));
		}

		@Override
		public void onSelect() {
			List<SelectItemDialog.Option> options = SelectItemOption.forMaterialAndQuality(subGroup, entityRenderer, messageDispatcher, i18nTranslator, onWeaponSelect, managementSkin);
			messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, new SelectItemDialog(i18nTranslator.getTranslatedString("GUI.SETTLER_MANAGEMENT.CHOOSE_WEAPON"),
					menuSkin, messageDispatcher, soundAssetDictionary, tooltipFactory, options, 6, "EquipWeapon"));
		}
	}

	//nearly used optional, but kept consistent of nulls
	private WeaponInfo getWeaponInfo(Entity weapon) {
		if (weapon.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
			return attributes.getItemType().getWeaponInfo();
		}
		return null;
	}

	private DefenseInfo getDefenseInfo(Entity entity) {
		if (entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
			return attributes.getItemType().getDefenseInfo();
		}
		return null;
	}

	private Table militarySquad(Entity settler) {
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);

		Table table = new Table();
		if (militaryComponent != null && militaryComponent.getSquadId() != null && gameContext.getSquads().containsKey(militaryComponent.getSquadId())) {
			Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());

			Drawable emblemDrawable = managementSkin.getDrawable(managementSkin.getEmblemName(squad));
			Image emblem = new Image(emblemDrawable);

			Label squadName = new Label(squad.getName(), managementSkin, "military_squad_name_ribbon");
			squadName.setAlignment(Align.center);
			table.add(squadName).padBottom(5).row();
			table.add(emblem);
		}

		return table;
	}

	public Updatable<Table> militaryToggle(Entity settler, boolean includeRibbon, Consumer<Entity> onMilitaryChange) {
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);

		Image image = new Image(managementSkin.getDrawable("icon_military"));
		ImageTextButton toggle = widgetFactory.createLeftLabelledToggle("GUI.SETTLER_MANAGEMENT.PROFESSION.MILITARY", managementSkin, image);
		toggle.setChecked(IS_MILITARY.test(settler));
		Consumer<Squad> assignSquadCallback = squad -> {
			militaryComponent.addToMilitary(squad.getId());
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(conscriptSoundAsset));
			onMilitaryChange.accept(settler);
		};
		toggle.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				boolean checked = toggle.isChecked();
				if (checked) {
					if (gameContext.getSquads().isEmpty()) {
						messageDispatcher.dispatchMessage(MessageType.MILITARY_CREATE_SQUAD_DIALOG, new MessageType.MilitaryCreateSquadDialogMessage(assignSquadCallback));
					} else {
						messageDispatcher.dispatchMessage(MessageType.MILITARY_SELECT_SQUAD_DIALOG, new MessageType.MilitarySelectSquadDialogMessage(assignSquadCallback));
					}
				} else {
					militaryComponent.removeFromMilitary();
				}
				onMilitaryChange.accept(settler);
			}
		});

		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
		String militaryProficiencyText = "";
		if (skillsComponent != null) {
			militaryProficiencyText = getAssignedWeaponText(settler, skillsComponent);
		}

		Label militaryProficiencyLabel = new Label(militaryProficiencyText, managementSkin, "military_highest_proficiency_label");
		militaryProficiencyLabel.setAlignment(Align.center);

		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);
		updatable.regularly(() -> {
			EntityAttributes attributes = settler.getPhysicalEntityComponent().getAttributes();
			if (attributes instanceof CreatureEntityAttributes creatureEntityAttributes) {
				boolean isSane = creatureEntityAttributes.getSanity() == Sanity.SANE;
				if (table.hasChildren()) {
					if (!isSane) {
						table.clearChildren();
					}
				} else {
					if (isSane) {
						table.add(toggle).spaceBottom(14).row();
						if (includeRibbon) {
							table.add(militaryProficiencyLabel);
						}
					}
				}


			}

		});
		updatable.update();
		return updatable;
	}

	public Table professions(Entity settler, float scale, Consumer<Entity> rebuildSettlerView) {
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
		if (skillsComponent == null) {
			return new Table();
		}

		Table table = new Table();
		settlerProfessionFactory.addProfessionComponents(settler, table, rebuildSettlerView, scale);
		return table;
	}


	public Updatable<Table> needs(Entity settler) {
		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);
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
					ProgressBar.ProgressBarStyle clonedStyle = new ProgressBar.ProgressBarStyle(progressBar.getStyle());

					if (clonedStyle.knobBefore instanceof NinePatchDrawable ninePatchDrawable) {
						Color progressBarColour;
						if (needValue >= 55f) {
							progressBarColour = ColorMixer.interpolate(55f, (float) NeedsComponent.MAX_NEED_VALUE, needValue.floatValue(), managementSkin.getColor("progress_bar_yellow"), managementSkin.getColor("progress_bar_green"));
						} else {
							progressBarColour = ColorMixer.interpolate((float) NeedsComponent.MIN_NEED_VALUE, 55f, needValue.floatValue(), managementSkin.getColor("progress_bar_red"), managementSkin.getColor("progress_bar_yellow"));
						}
						clonedStyle.knobBefore = ninePatchDrawable.tint(progressBarColour);
					}
					progressBar.setStyle(clonedStyle);

					updatable.regularly(() -> {
						Double newNeedValue = needsComponent.getValue(need);
						progressBar.setValue(Math.round(newNeedValue));

						if (clonedStyle.knobBefore instanceof NinePatchDrawable ninePatchDrawable) {
							Color progressBarColour;
							if (newNeedValue >= 55f) {
								progressBarColour = ColorMixer.interpolate(55f, (float) NeedsComponent.MAX_NEED_VALUE, newNeedValue.floatValue(), managementSkin.getColor("progress_bar_yellow"), managementSkin.getColor("progress_bar_green"));
							} else {
								progressBarColour = ColorMixer.interpolate((float) NeedsComponent.MIN_NEED_VALUE, 55f, newNeedValue.floatValue(), managementSkin.getColor("progress_bar_red"), managementSkin.getColor("progress_bar_yellow"));
							}
							clonedStyle.knobBefore = ninePatchDrawable.tint(progressBarColour);
						}
					});

					table.add(icon).right().spaceRight(28);
					table.add(progressBar).left().width(318).height(42);
					table.row();
				}
			}

		}
		return updatable;
	}

	public Updatable<Table> happiness(Entity settler) {
		HappinessComponent happinessComponent = settler.getComponent(HappinessComponent.class);
		Label happinessLabel = tableLabel("");

		Table modifiersTable = new Table();
		Runnable updateMethod = () -> {
			modifiersTable.clear();
			int netHappiness = happinessComponent.getNetModifier();
			String netHappinessString = (netHappiness > 0 ? "+" : "") + netHappiness;
			String happinessText = i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.HAPPINESS", Map.of(
					"happinessValue", new I18nWord(netHappinessString)
			)).toString();
			happinessLabel.setText(happinessText);
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
		};

		updateMethod.run();

		Table table = new Table();
		table.add(happinessLabel).spaceBottom(16f).row();
		table.add(modifiersTable);
		Updatable<Table> updatable = Updatable.of(table);
		updatable.regularly(updateMethod);

		return updatable;
	}

	public Table textSummary(Entity settler) {
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);

		String settlerName = getName(settler);
		String currentProfessionName = "";
		//TODO: if in military, get rank
		if (skillsComponent != null) {
			if (IS_MILITARY.test(settler)) {
				currentProfessionName = getAssignedWeaponText(settler, skillsComponent);
			} else {
				java.util.List<SkillsComponent.QuantifiedSkill> activeProfessions = skillsComponent.getActiveProfessions();
				if (!activeProfessions.isEmpty()) {
					SkillsComponent.QuantifiedSkill quantifiedSkill = activeProfessions.get(0);
					currentProfessionName = i18nTranslator.getSkilledProfessionDescription(quantifiedSkill.getSkill(), quantifiedSkill.getLevel(),
							((CreatureEntityAttributes) settler.getPhysicalEntityComponent().getAttributes()).getGender()).toString();
				}
			}
		}
		if (settler.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureEntityAttributes
				&& creatureEntityAttributes.getSanity() != Sanity.SANE) {
			String sanityText = i18nTranslator.translate(creatureEntityAttributes.getSanity().i18nKey);
			currentProfessionName = sanityText;
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

		Label professionLabel = new Label(currentProfessionName, managementSkin, "default-font-18-label");
		professionLabel.setAlignment(Align.left);
		table.add(professionLabel).growX().row();

		for (String behaviourDescription : behaviourDescriptions) {
			Label descriptionLabel = new Label(behaviourDescription, managementSkin, "default-font-18-label") {
				@Override
				public float getWidth() {
					return getParent().getWidth();
				}
			};
			descriptionLabel.setWrap(true);
			descriptionLabel.setAlignment(Align.left);
			table.add(descriptionLabel).growX().row();
		}

		return table;
	}

	public Table mugshot(Entity settler) {
		Table table = new Table();
		float scaleFactor = 0.9f;
		Drawable background = managementSkin.bgForExampleEntity(settler.getId());
		table.setBackground(background);
		EntityDrawable entityDrawable = new EntityDrawable(settler, entityRenderer, true, messageDispatcher);
		entityDrawable.setMinSize(background.getMinWidth() * scaleFactor, background.getMinHeight() * scaleFactor);
		table.add(new Image(entityDrawable));
		return table;
	}


	public String getAssignedWeaponText(Entity settler, SkillsComponent skillsComponent) {
		String militaryProficiencyText;
		Skill currentCombatSkill = SkillDictionary.UNARMED_COMBAT_SKILL;
		int currentCombatSkillLevel = skillsComponent.getSkillLevel(currentCombatSkill);
		MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
		if (militaryComponent != null) {
			Entity assignedWeapon = gameContext.getEntity(militaryComponent.getAssignedWeaponId());
			if (assignedWeapon != null) {
				WeaponInfo weaponInfo = getWeaponInfo(assignedWeapon);
				if (weaponInfo != null) {
					currentCombatSkill = weaponInfo.getCombatSkill();
					currentCombatSkillLevel = skillsComponent.getSkillLevel(weaponInfo.getCombatSkill());
				}
			}
		}

		militaryProficiencyText = i18nTranslator.getSkilledProfessionDescription(currentCombatSkill, currentCombatSkillLevel,
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

	protected void disable(Actor actor) {
		actor.addAction(Actions.alpha(0.5f));
		actor.setTouchable(Touchable.disabled);
	}

	protected void enable(Actor actor) {
		actor.clearActions();
		actor.addAction(Actions.alpha(1f));
		actor.setTouchable(Touchable.enabled);
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
