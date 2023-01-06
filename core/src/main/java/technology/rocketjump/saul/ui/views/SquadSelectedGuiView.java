package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.SquadOrderChangeMessage;
import technology.rocketjump.saul.military.MilitaryMessageHandler;
import technology.rocketjump.saul.military.model.MilitaryShift;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.military.model.SquadOrderType;
import technology.rocketjump.saul.screens.SettlerManagementScreen;
import technology.rocketjump.saul.settlement.SettlerTracker;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.Updatable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.*;

import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static technology.rocketjump.saul.military.model.SquadOrderType.TRAINING;

@Singleton
public class SquadSelectedGuiView implements GuiView, GameContextAware {

	private static final String[] DEFAULT_SQUAD_EMBLEMS = {
			"icon_military_emblem_hammer",
			"icon_military_emblem_snow",
			"icon_military_emblem_wolf",
			"icon_military_emblem_tree",
			"icon_military_emblem_helmet",
			"icon_military_emblem_fire",
			"icon_military_emblem_arrow",
			"icon_military_emblem_skull"
	};

	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final MainGameSkin mainGameSkin;
	private final ManagementSkin managementSkin;
	private final MenuSkin menuSkin;
	private final ButtonFactory buttonFactory;
	private final TooltipFactory tooltipFactory;
	private final SoundAssetDictionary soundAssetDictionary;
	private final WidgetFactory widgetFactory;
	private final SettlerTracker settlerTracker;
	private final SettlerManagementScreen settlerManagementScreen; //TODO: this shouldn't be here

	private GameContext gameContext;
	private Tabs selectedTab = Tabs.SQUADS;
	private List<Updatable<?>> updatables;
	private Table containerTable;

	enum Tabs {
		SQUADS("icon_military_tabs_squads", "GUI.MILITARY.TAB.SQUADS"),
		DWARVES("icon_military_tabs_dwarves", "GUI.MILITARY.TAB.DWARVES"),
		TRAINED_CIVILIANS( "icon_military_tabs_trained_civs", "GUI.MILITARY.TAB.TRAINED_CIVILIANS");

		private final String drawableName;
		private final String i18nKey;

		Tabs(String drawableName, String i18nKey) {
			this.drawableName = drawableName;
			this.i18nKey = i18nKey;
		}
	}

	enum SquadCommand {
		TRAIN("icon_military_orders_training", MessageType.MILITARY_SQUAD_ORDERS_CHANGED, squad -> new SquadOrderChangeMessage(squad, TRAINING)),
		GUARD("icon_military_orders_move", MessageType.GUI_SWITCH_INTERACTION_MODE, x -> GameInteractionMode.SQUAD_MOVE_TO_LOCATION),
		ATTACK("icon_military_orders_attack", MessageType.GUI_SWITCH_INTERACTION_MODE, x -> GameInteractionMode.SQUAD_ATTACK_CREATURE),
		CANCEL_ATTACK("icon_military_orders_stand_down", MessageType.GUI_SWITCH_INTERACTION_MODE, x -> GameInteractionMode.CANCEL_ATTACK_CREATURE),
		RETREAT("icon_military_orders_retreat", MessageType.MILITARY_SQUAD_ORDERS_CHANGED, squad -> new SquadOrderChangeMessage(squad, SquadOrderType.RETREATING));

		final String drawableName;
		final int messageType;
		final Function<Squad, Object> messageF;

		SquadCommand(String drawableName, int messageType, Function<Squad, Object> messageFunction) {
			this.drawableName = drawableName;
			this.messageType = messageType;
			this.messageF = messageFunction;
		}

		public void apply(GameInteractionStateContainer gameInteractionStateContainer, MessageDispatcher messageDispatcher) {
			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
			if (squad != null) {
				messageDispatcher.dispatchMessage(messageType, messageF.apply(squad));
			}
		}
	}

	@Inject
	public SquadSelectedGuiView(GuiSkinRepository guiSkinRepository, GameInteractionStateContainer gameInteractionStateContainer,
	                            I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher,
	                            ButtonFactory buttonFactory,
	                            TooltipFactory tooltipFactory, SoundAssetDictionary soundAssetDictionary, WidgetFactory widgetFactory,
	                            SettlerTracker settlerTracker, SettlerManagementScreen settlerManagementScreen) {

		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.messageDispatcher = messageDispatcher;
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.buttonFactory = buttonFactory;
		this.tooltipFactory = tooltipFactory;
		this.soundAssetDictionary = soundAssetDictionary;
		this.widgetFactory = widgetFactory;
		this.settlerTracker = settlerTracker;
		this.settlerManagementScreen = settlerManagementScreen;
	}

	@Override
	public void populate(Table containerTable) {
		this.containerTable = containerTable;
		containerTable.clear();
		updatables = new ArrayList<>();


		Label title = new Label(i18nTranslator.translate("GUI.SETTLER_MANAGEMENT.PROFESSION.MILITARY"), managementSkin, "military_title_ribbon");
		title.setAlignment(Align.center);

		Updatable<Table> squadSummaryGrid = squadSummaries();
		updatables.add(squadSummaryGrid);

		Table tabButtons = tabButtons();

		Updatable<Table> currentTab = currentTab();
		updatables.add(currentTab);

		Table outerTable = new Table();
		outerTable.setTouchable(Touchable.enabled);
		outerTable.setBackground(managementSkin.getDrawable("trade_bg_left")); //Doesn't fill screen due to aspect ratio

		outerTable.add(title).padTop(40).width(800).row();
		outerTable.add(squadSummaryGrid.getActor()).padTop(20).row();
		outerTable.add(tabButtons).padTop(20).row();
		outerTable.add(currentTab.getActor()).padTop(20).growY().row();

		containerTable.add(outerTable).growY();
		update();
	}

	private Updatable<Table> currentTab() {
		Table squadTab = squadTab();
		Table soldiersTab = soldiersTab();

		Table currentTab = new Table();
		Updatable<Table> updatable = Updatable.of(currentTab);
		updatable.regularly(new Runnable() {
			Tabs previousTab;

			@Override
			public void run() {
				//TODO: not entirely enthusiastic about this design, but works in the framework
				if (previousTab != SquadSelectedGuiView.this.selectedTab) {
					previousTab = SquadSelectedGuiView.this.selectedTab;
					currentTab.clear();

					switch (previousTab) {
						case SQUADS -> {
							currentTab.add(squadTab).grow();
						}
						case DWARVES -> {
							currentTab.add(soldiersTab).grow();
						}
						case TRAINED_CIVILIANS -> {
						}
					}

				}
			}
		});

		return updatable;
	}

	private Table soldiersTab() {
		Label subtitle = new Label(i18nTranslator.translate(Tabs.DWARVES.i18nKey), managementSkin, "military_subtitle_ribbon");
		subtitle.setAlignment(Align.center);

		Table subtitleLine = new Table();
		subtitleLine.add(subtitle).padLeft(30).padRight(30);

		Table soldiersTable = new Table();
		soldiersTable.top();
		soldiersTable.defaults().spaceTop(10).spaceBottom(140).padRight(20).padLeft(20);

		Updatable<Table> updatable = Updatable.of(soldiersTable);

		updatable.regularly(() -> {
			soldiersTable.clear();
			List<Entity> soldiers = settlerTracker.getLiving()
					.stream()
					.filter(SettlerManagementScreen.IS_MILITARY)
					.sorted(SettlerManagementScreen.SORT_MILITARY_CIVILIAN)
					.toList();

			for (Entity soldier : soldiers) {
				MilitaryComponent militaryComponent = soldier.getComponent(MilitaryComponent.class);
				Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());

				Drawable emblemDrawable = managementSkin.getDrawable(getEmblemName(squad));
				Image emblem = new Image(emblemDrawable);
				emblem.setTouchable(Touchable.enabled);
				tooltipFactory.simpleTooltip(emblem, new I18nText(squad.getName()), TooltipLocationHint.ABOVE);

				//TODO: ashamed of not refactoring this properly
				soldiersTable.add(settlerManagementScreen.mugshot(soldier)).top().spaceRight(50).spaceLeft(50);
				soldiersTable.add(settlerManagementScreen.textSummary(soldier)).left().growX().spaceRight(50);
				soldiersTable.add(emblem).top();
				soldiersTable.row();
			}
		});

		updatable.update();
		updatables.add(updatable);

		ScrollPane scrollPane = new EnhancedScrollPane(soldiersTable, menuSkin);
		scrollPane.setFadeScrollBars(false);


		Table table = new Table();
		table.add(subtitleLine).padTop(30).padBottom(30f).row();
		table.add(scrollPane).grow().width(1100).padBottom(30f).row();


		return table;
	}

	private Table squadTab() {
		Table table = new Table();

		Label subtitle = new Label(i18nTranslator.translate(Tabs.SQUADS.i18nKey), managementSkin, "military_subtitle_ribbon");
		subtitle.setAlignment(Align.center);
		Updatable<TextButton> addSquadButton = addSquadButton();
		updatables.add(addSquadButton);

		Table subtitleLine = new Table();
		subtitleLine.add(new Container<>()).width(addSquadButton.getActor().getMinWidth());
		subtitleLine.add(subtitle).padLeft(30).padRight(30);
		subtitleLine.add(addSquadButton.getActor());


		ButtonGroup<Button> squadCardButtonGroup = new ButtonGroup<>();
		Table squadCardsTable = new Table();
		squadCardsTable.defaults().spaceTop(10).spaceBottom(10).padRight(20);
		ScrollPane cardsScrollPane = new EnhancedScrollPane(squadCardsTable, menuSkin);
		cardsScrollPane.setFadeScrollBars(false);

		for (Squad squad : getSquads()) {
			Button squadCard = squadCard(squad);
			squadCardButtonGroup.add(squadCard);
			squadCardsTable.add(squadCard).spaceTop(20).spaceBottom(20).row();
		}

		//TODO: why doesn't this want to scroll to the right place
//		cardsScrollPane.layout();
//		Button checkedSquadCard = squadCardButtonGroup.getChecked();
//		cardsScrollPane.scrollTo(checkedSquadCard.getX(), checkedSquadCard.getY(), checkedSquadCard.getMinWidth(), checkedSquadCard.getMinHeight(), true, true);

		squadCardsTable.top();

		Label ordersLabel = new Label(i18nTranslator.translate("GUI.MILITARY.ORDERS"), managementSkin, "military_subtitle_ribbon");
		ordersLabel.setAlignment(Align.center);
		Updatable<Table> orderButtons = orderButtons();
		updatables.add(orderButtons);

		table.add(subtitleLine).padTop(30).padBottom(30f).row();
		table.add(cardsScrollPane).grow().row();

		table.add(ordersLabel).padTop(30).padBottom(30f).row();
		table.add(orderButtons.getActor()).padBottom(30f).row();
		return table;
	}

	private Button squadCard(Squad squad) {
		Button card = new Button(managementSkin, "squad_card");
		card.addCaptureListener(event -> {
			if (event instanceof InputEvent inputEvent) {
				switch (inputEvent.getType()) {
					case touchDown:
					case touchUp:
					case touchDragged:
						gameInteractionStateContainer.setSelectable(new Selectable(squad)); //current select message resets gui view
						update();
						card.setChecked(true);
					default:
				}
			}
			return false;
		});
		if (gameInteractionStateContainer.getSelectable() != null && squad.equals(gameInteractionStateContainer.getSelectable().getSquad())) {
			card.setChecked(true);
		}


		Label personnelCountLabel = new Label(String.valueOf(squad.getMemberEntityIds().size()), managementSkin, "entity_drawable_quantity_label");
		personnelCountLabel.setAlignment(Align.center);

		Image personnelIcon = new Image(managementSkin.getDrawable("icon_soldier_amount"));
		HorizontalGroup personnelWidget = new HorizontalGroup();
		personnelWidget.space(5);
		personnelWidget.addActor(personnelCountLabel);
		personnelWidget.addActor(personnelIcon);

		Label squadNameLabel = new Label(squad.getName(), mainGameSkin, "title-header");
		Updatable<Label> updatableSquadNameLabel = Updatable.of(squadNameLabel);
		updatableSquadNameLabel.regularly(() -> {
			squadNameLabel.setText(squad.getName());
		});
		updatables.add(updatableSquadNameLabel);

		HorizontalGroup titleWidget = new HorizontalGroup();
		titleWidget.space(5);
		titleWidget.addActor(squadNameLabel);
		titleWidget.addActor(renameSquadButton(squad));



		Table emblemColumn = new Table();
		Updatable<Table> updatableEmblemColumn = Updatable.of(emblemColumn);
		updatableEmblemColumn.regularly(() -> {
			emblemColumn.clear();
			emblemColumn.add(selectableEmblem(squad)).row();
			for (I18nText line : squad.getDescription(i18nTranslator, gameContext, messageDispatcher)) {
				Label emblemDescription = new Label(line.toString(), managementSkin, "default-font-16-label-white");
				emblemColumn.add(emblemDescription).row();
			}
		});
		updatables.add(updatableEmblemColumn);

		Updatable<Button> removeSquadButton = removeSquadButton(squad);
		updatables.add(removeSquadButton);

		Updatable<SelectBox<SquadCommand>> squadCommandSelect = squadCommandSelect(squad);
		updatables.add(squadCommandSelect);

		Table squadActionColumn = new Table();
		squadActionColumn.add(shiftToggle(squad)).row();
		squadActionColumn.add(new Label(i18nTranslator.translate("GUI.MILITARY.SET_FORMATION"), managementSkin, "default-font-16-label-white")).padTop(24).row();
		squadActionColumn.add(widgetFactory.createSquadFormationSelectBox(menuSkin, squad.getFormation(), squad::setFormation)).width(440).padTop(16f).row();
		squadActionColumn.add(new Label(i18nTranslator.translate("GUI.MILITARY.SET_ORDERS"), managementSkin, "default-font-16-label-white")).padTop(24).row();
		squadActionColumn.add(squadCommandSelect.getActor()).padTop(16f).growX();

		card.add(personnelWidget).left().padLeft(20).padBottom(15);
		card.add(new Container<>(titleWidget)).growX().padBottom(15);
		card.add(removeSquadButton.getActor()).right().padRight(20).padBottom(15);
		card.row();
		card.add(emblemColumn).uniformX().padLeft(38);
		card.add(squadActionColumn).padLeft(30).padRight(30);
		card.add(new Container<>()).uniformX().padRight(38);
		return card;
	}

	private Updatable<SelectBox<SquadCommand>> squadCommandSelect(Squad squad) {
		SelectBox<SquadCommand> select = new SelectBox<>(menuSkin, "select_narrow") {
			@Override
			protected String toString(SquadCommand item) {
				return translate(item);
			}
		};
		select.setAlignment(Align.center);
		select.getList().setAlignment(Align.center);

		select.setItems(SquadCommand.values());

		select.getSelection().setProgrammaticChangeEvents(false);
		select.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				select.getSelected().apply(gameInteractionStateContainer, messageDispatcher);
			}
		});
		Updatable<SelectBox<SquadCommand>> updatable = Updatable.of(select);
		updatable.regularly(new Runnable() {
			SquadOrderType previousOrder = null;

			@Override
			public void run() {
				if (previousOrder != squad.getCurrentOrderType()) {
					previousOrder = squad.getCurrentOrderType();
					switch (squad.getCurrentOrderType()) {
						case TRAINING -> select.setSelected(SquadCommand.TRAIN);
						case GUARDING -> select.setSelected(SquadCommand.GUARD);
						case COMBAT -> select.setSelected(SquadCommand.ATTACK);
						case RETREATING -> select.setSelected(SquadCommand.RETREAT);
					}

				}
			}
		});
		updatable.update();
		return updatable;
	}

	private String translate(SquadCommand item) {
		return i18nTranslator.translate("GUI.MILITARY.ORDERS." + item.name());
	}

	private Actor shiftToggle(Squad squad) {
		Label dayShiftLabel = new Label(i18nTranslator.translate(MilitaryShift.DAYTIME.getI18nKey()), managementSkin, "default-font-16-label-white");
		Label nightShiftLabel = new Label(i18nTranslator.translate(MilitaryShift.NIGHTTIME.getI18nKey()), managementSkin, "default-font-16-label-white");
		Button toggle = new Button(managementSkin, "toggle");
		toggle.setChecked(squad.getShift() == MilitaryShift.NIGHTTIME);

		Consumer<MilitaryShift> toggleLabels = militaryShift -> {
			dayShiftLabel.setTouchable(Touchable.disabled);
			nightShiftLabel.setTouchable(Touchable.disabled);
			dayShiftLabel.getColor().a = 0.6f;
			nightShiftLabel.getColor().a = 0.6f;
			if (militaryShift == MilitaryShift.DAYTIME) {
				dayShiftLabel.getColor().a = 1f;
				nightShiftLabel.setTouchable(Touchable.enabled);
			} else {
				nightShiftLabel.getColor().a = 1f;
				dayShiftLabel.setTouchable(Touchable.enabled);
			}
		 };

		toggle.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (toggle.isChecked()) {
					squad.setShift(MilitaryShift.NIGHTTIME);
				} else {
					squad.setShift(MilitaryShift.DAYTIME);
				}
				toggleLabels.accept(squad.getShift());
				messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_SHIFT_CHANGED, squad);
			}
		});

		toggleLabels.accept(squad.getShift());
		buttonFactory.attachClickCursor(toggle, GameCursor.SELECT);
		buttonFactory.attachClickCursor(dayShiftLabel, GameCursor.SELECT);
		buttonFactory.attachClickCursor(nightShiftLabel, GameCursor.SELECT);
		dayShiftLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				toggle.setChecked(false);

			}
		});

		nightShiftLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				toggle.setChecked(true);
			}
		});

		Table shiftWidget = new Table();
		shiftWidget.add(dayShiftLabel);
		shiftWidget.add(toggle);
		shiftWidget.add(nightShiftLabel);
		return shiftWidget;
	}

	private Actor renameSquadButton(Squad squad) {
		Drawable changeButtonDrawable = mainGameSkin.getDrawable("icon_edit");
		Button changeNameButton = new Button(changeButtonDrawable);
		changeNameButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				I18nText dialogTitle = i18nTranslator.getTranslatedString("GUI.MILITARY.DIALOG.RENAME_SQUAD");
				I18nText buttonText = i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

				String originalName = squad.getName();

				TextInputDialog textInputDialog = new TextInputDialog(dialogTitle, originalName, buttonText, menuSkin, (newName) -> {
					if (!originalName.equals(newName) && !newName.isEmpty()) {
						squad.setName(newName);
					}
				}, messageDispatcher, soundAssetDictionary);
				textInputDialog.setMaxLength(20);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, textInputDialog);
			}
		});
		tooltipFactory.simpleTooltip(changeNameButton, "GUI.MILITARY.DIALOG.RENAME_SQUAD", TooltipLocationHint.ABOVE);
		changeNameButton.addListener(new ChangeCursorOnHover(changeNameButton, GameCursor.SELECT, messageDispatcher));
		return changeNameButton;
	}

	private Updatable<Button> removeSquadButton(Squad squad) {
		GameDialog removeSquadDialog = new GameDialog(i18nTranslator.getTranslatedString("GUI.MILITARY.REMOVE_SQUAD"), menuSkin, messageDispatcher, soundAssetDictionary) {
			@Override
			public void dispose() { }
		};
		removeSquadDialog.withText(i18nTranslator.getTranslatedString("GUI.MILITARY.REMOVE_SQUAD.DIALOG"));
		removeSquadDialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.CANCEL_BUTTON"));
		removeSquadDialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON"), () -> {
			messageDispatcher.dispatchMessage(MessageType.MILITARY_REMOVE_SQUAD, squad);
			populate(containerTable);
		});


		Button removeSquadButton = new Button(managementSkin, "remove_button");
		removeSquadButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, removeSquadDialog);
			}
		});
		tooltipFactory.simpleTooltip(removeSquadButton,"GUI.MILITARY.REMOVE_SQUAD", TooltipLocationHint.ABOVE);
		buttonFactory.attachClickCursor(removeSquadButton, GameCursor.SELECT);
		Updatable<Button> updatable = Updatable.of(removeSquadButton);
		updatable.regularly(() -> {
			if (gameContext.getSquads().size() > 1) {
				buttonFactory.enable(removeSquadButton);
			} else {
				buttonFactory.disable(removeSquadButton);
			}
		});
		updatable.update();
		return updatable;
	}

	private Updatable<TextButton> addSquadButton() {
		TextButton addSquadButton = new TextButton(i18nTranslator.translate("GUI.MILITARY.NEW_SQUAD"), managementSkin, "military_text_button");
		addSquadButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				I18nText dialogTitle = i18nTranslator.getTranslatedString("GUI.MILITARY.DIALOG.ADD_SQUAD");
				I18nText buttonText = i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON");
				long nextSquadId = gameContext.getSquads().keySet().stream().mapToLong(Long::longValue).max().orElse(0) + 1;
				String suggestedSquadName = i18nTranslator.getTranslatedString("MILITARY.SQUAD.DEFAULT_NAME") + " #" + nextSquadId;
				TextInputDialog textInputDialog = new TextInputDialog(dialogTitle, suggestedSquadName, buttonText, menuSkin, (newName) -> {
					if (!newName.isEmpty()) {
						Squad newSquad = new Squad();
						newSquad.setId(nextSquadId);
						newSquad.setName(newName);
						newSquad.setEmblemName(getEmblemName(newSquad));

						messageDispatcher.dispatchMessage(MessageType.MILITARY_CREATE_SQUAD, newSquad);
						messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(newSquad));
					}
				}, messageDispatcher, soundAssetDictionary);
				textInputDialog.setMaxLength(20);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, textInputDialog);
			}
		});
		buttonFactory.attachClickCursor(addSquadButton, GameCursor.SELECT);
		Updatable<TextButton> updatable = Updatable.of(addSquadButton);
		updatable.regularly(() -> {
			if (gameContext.getSquads().size() < MilitaryMessageHandler.MAX_SQUAD_COUNT) {
				buttonFactory.enable(addSquadButton);
			} else {
				buttonFactory.disable(addSquadButton);
			}
		});
		updatable.update();
		return updatable;
	}


	private Actor selectableEmblem(Squad squad) {
		Drawable emblemDrawable = managementSkin.getDrawable(getEmblemName(squad));
		Image emblem = new Image(emblemDrawable);
		emblem.setTouchable(Touchable.enabled);
		buttonFactory.attachClickCursor(emblem, GameCursor.SELECT);

		emblem.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				List<EmblemOption> options = Arrays.stream(DEFAULT_SQUAD_EMBLEMS)
						.map(drawableName -> new EmblemOption(squad, drawableName, managementSkin))
						.toList();


				I18nText title = i18nTranslator.getTranslatedWordWithReplacements("GUI.MILITARY.DIALOG.CHOOSE_EMBLEM", Map.of("squadName", new I18nText(squad.getName())));
				SelectItemDialog dialog = new SelectItemDialog(title, menuSkin, messageDispatcher, soundAssetDictionary, tooltipFactory, options, 4);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
			}
		});

		return emblem;
	}

	class EmblemOption extends SelectItemDialog.Option {

		private final Squad squad;
		private final String drawableName;
		private final Skin skin;

		public EmblemOption(Squad squad, String drawableName, Skin skin) {
			super(I18nText.BLANK);
			this.squad = squad;
			this.drawableName = drawableName;
			this.skin = skin;
		}

		@Override
		public void addSelectionComponents(Table innerTable) {
			Image emblem = new Image(skin.getDrawable(drawableName));
			innerTable.add(emblem).pad(20);
		}

		@Override
		public void onSelect() {
			this.squad.setEmblemName(drawableName);
		}
	}

	private Table tabButtons() {
		ButtonGroup<ImageButton> buttonGroup = new ButtonGroup<>();
		Table table = new Table();
		for (Tabs tab : Tabs.values()) {
			ImageButton button = buttonFactory.checkableButton(managementSkin.getDrawable(tab.drawableName), true);
			button.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (button.isChecked()) {
						SquadSelectedGuiView.this.selectedTab = tab;
						update();
					}
				}
			});
			tooltipFactory.simpleTooltip(button, tab.i18nKey, TooltipLocationHint.ABOVE);
			buttonGroup.add(button);
			table.add(button);
		}

		return table;
	}

	private Updatable<Table> orderButtons() {
		ButtonGroup<ImageButton> buttonGroup = new ButtonGroup<>();
		Table table = new Table();
		Map<SquadCommand, ImageButton> commandToButton = new HashMap<>();
		for (SquadCommand squadCommand : SquadCommand.values()) {
			ImageButton button = buttonFactory.checkableButton(managementSkin.getDrawable(squadCommand.drawableName), true);
			button.setProgrammaticChangeEvents(false);
			button.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					super.clicked(event, x, y);
					squadCommand.apply(gameInteractionStateContainer, messageDispatcher);
				}
			});
			tooltipFactory.simpleTooltip(button, translate(squadCommand), TooltipLocationHint.ABOVE);
			buttonGroup.add(button);
			table.add(button);
			commandToButton.put(squadCommand, button);
		}

		Updatable<Table> updatable = Updatable.of(table);
		updatable.regularly(new Runnable() {
			SquadOrderType previousOrder = null;
			@Override
			public void run() {
				Selectable selectable = gameInteractionStateContainer.getSelectable();
				if (selectable != null && selectable.getSquad() != null) {
					Squad squad = selectable.getSquad();
					if (previousOrder != squad.getCurrentOrderType()) {
						previousOrder = squad.getCurrentOrderType();
						switch (squad.getCurrentOrderType()) {
							case TRAINING -> commandToButton.get(SquadCommand.TRAIN).setChecked(true);
							case GUARDING -> commandToButton.get(SquadCommand.GUARD).setChecked(true);
							case COMBAT -> commandToButton.get(SquadCommand.ATTACK).setChecked(true);
							case RETREATING -> commandToButton.get(SquadCommand.RETREAT).setChecked(true);
						}
					}
				}
			}
		});
		return updatable;
	}

	private Updatable<Table> squadSummaries() {
		Table table = new Table();
		table.defaults().uniform().left().space(30);
		Updatable<Table> updatable = Updatable.of(table);
		updatable.regularly(() -> {
			table.clear();
			for (List<Squad> squadRow : Lists.partition(getSquads(), 3)) {
				for (Squad squad : squadRow) {
					Table summary = squadSummary(squad);
					table.add(summary);
				}
				table.row();
			}
		});
		updatable.update();
		return updatable;
	}

	private List<Squad> getSquads() {
		if (gameContext != null) {
			Map<Long, Squad> squadMap = gameContext.getSquads();
			return squadMap.keySet().stream().sorted().map(squadMap::get).toList();
		} else {
			return Collections.emptyList();
		}
	}

	private Table squadSummary(Squad squad) {
		String smallEmblemName = getSmallEmblemName(squad);
		String squadName = squad.getName();
		List<I18nText> descriptions = squad.getDescription(i18nTranslator, gameContext, messageDispatcher);

		Image smallEmblem = new Image(managementSkin.getDrawable(smallEmblemName));

		Table textTable = new Table();
		textTable.defaults().left();
		textTable.add(new Label(squadName, managementSkin, "default-font-18-label")).row();
		for (I18nText i18nText : descriptions) {
			textTable.add(new Label(i18nText.toString(), managementSkin, "default-font-18-label")).row();
		}

		Table table = new Table();
		table.add(smallEmblem);
		table.add(textTable).left().spaceLeft(10);

		return table;
	}

	private String getEmblemName(Squad squad) {
		String emblemName = squad.getEmblemName();
		if (emblemName != null) {
			return emblemName;
		} else {
			return DEFAULT_SQUAD_EMBLEMS[(int) ((squad.getId() - 1) % DEFAULT_SQUAD_EMBLEMS.length)]; //code duplication, was tempted to set on the squad
		}
	}

	private String getSmallEmblemName(Squad squad) {
		String smallEmblemName = squad.getSmallEmblemName();
		if (smallEmblemName != null) {
			return smallEmblemName;
		} else {
			return DEFAULT_SQUAD_EMBLEMS[(int) ((squad.getId() - 1) % DEFAULT_SQUAD_EMBLEMS.length)] + "_small"; //code duplication, was tempted to set on the squad
		}
	}

	/**
	 * Updates every second or so, not instant on show
	 */
	@Override
	public void update() {
		int sizeBeforeUpdate = updatables.size();
		for (Updatable<?> updatable : updatables) {
			updatable.update();
		}
		int sizeAfterUpdate = updatables.size();
		if (sizeAfterUpdate > sizeBeforeUpdate) {
			Logger.error("Leak in {} as more updatbles {} since {}", getClass().getSimpleName(), sizeAfterUpdate, sizeBeforeUpdate);
		}
	}


	@Override
	public GuiViewName getName() {
		return GuiViewName.SQUAD_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}


	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
