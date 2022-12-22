package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.military.SquadFormationDictionary;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Updatable;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;
import technology.rocketjump.saul.ui.widgets.EnhancedScrollPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

	private GameContext gameContext;
	private Tabs selectedTab = Tabs.SQUADS;
	private List<Updatable<?>> updatables;

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

	@Inject
	public SquadSelectedGuiView(GuiSkinRepository guiSkinRepository, GameInteractionStateContainer gameInteractionStateContainer,
	                            I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher,
	                            SquadFormationDictionary squadFormationDictionary, ButtonFactory buttonFactory,
	                            TooltipFactory tooltipFactory) {

		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.messageDispatcher = messageDispatcher;
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.buttonFactory = buttonFactory;
		this.tooltipFactory = tooltipFactory;
	}

	@Override
	public void populate(Table containerTable) {
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

		containerTable.add(outerTable);
	}

	private Updatable<Table> currentTab() {
		Table squadTab = squadTab();

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

						}
						case TRAINED_CIVILIANS -> {
						}
					}

				}
			}
		});

		currentTab.debugAll();
		return updatable;
	}

	private Table squadTab() {
		Table table = new Table();
		Label subtitle = new Label(i18nTranslator.translate(Tabs.SQUADS.i18nKey), managementSkin, "military_subtitle_ribbon");
		subtitle.setAlignment(Align.center);

		ButtonGroup<Button> squadCardButtonGroup = new ButtonGroup<>();
		Table squadCardsTable = new Table();
		squadCardsTable.defaults().spaceTop(10).spaceBottom(10);

		for (Squad squad : getSquads()) {
			Button squadCard = squadCard(squad);
			squadCardButtonGroup.add(squadCard);
			squadCardsTable.add(squadCard).row();
		}

		squadCardsTable.top();
		ScrollPane cardsScrollPane = new EnhancedScrollPane(squadCardsTable, menuSkin);


		Label ordersLabel = new Label(i18nTranslator.translate("GUI.MILITARY.ORDERS"), managementSkin, "military_subtitle_ribbon");
		ordersLabel.setAlignment(Align.center);

		table.add(subtitle).row(); //TODO add remove button
		table.add(cardsScrollPane).grow().row();

		table.add(ordersLabel).row();
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
						//TODO: update orders at bottom
						card.setChecked(true);
					default:
				}
			}
			return false;
		});


		Label personnelCountLabel = new Label(String.valueOf(squad.getMemberEntityIds().size()), managementSkin, "entity_drawable_quantity_label");
		personnelCountLabel.setAlignment(Align.center);

		Image personnelIcon = new Image(managementSkin.getDrawable("icon_soldier_amount"));
		HorizontalGroup personnelWidget = new HorizontalGroup();
		personnelWidget.space(5);
		personnelWidget.addActor(personnelCountLabel);
		personnelWidget.addActor(personnelIcon);

		HorizontalGroup titleWidget = new HorizontalGroup();
		titleWidget.space(5);


		Table titleRow = new Table();
		titleRow.add(personnelWidget);


		Table emblemColumn = new Table();
		Drawable emblemDrawable = managementSkin.getDrawable(getEmblemName(squad));
		Image emblem = new Image(emblemDrawable);
		emblemColumn.add(emblem).row();

		for (I18nText line : squad.getDescription(i18nTranslator, gameContext, messageDispatcher)) {
			Label emblemDescription = new Label(line.toString(), managementSkin, "default-font-16-label-white");
			emblemColumn.add(emblemDescription).row();
		}

		//TODO: change emblem button wizard

		Table contentsRow = new Table();
		contentsRow.add(emblemColumn);

		card.add(titleRow).row();
		card.add(contentsRow).row();


		return card;
	}

	private Table tabButtons() {
		ButtonGroup<ImageButton> buttonGroup = new ButtonGroup<>();
		Table table = new Table();
		for (Tabs tab : Tabs.values()) {
			ImageButton button = buttonFactory.checkableButton(managementSkin.getDrawable(tab.drawableName));
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

	private Updatable<Table> squadSummaries() {
		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);
		updatable.regularly(() -> {
			table.clear();
			for (List<Squad> squadRow : Lists.partition(getSquads(), 3)) {
				for (Squad squad : squadRow) {
					Table summary = squadSummary(squad);
					table.add(summary).spaceLeft(30).spaceRight(30);
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
			return DEFAULT_SQUAD_EMBLEMS[(int) (squad.getId() % DEFAULT_SQUAD_EMBLEMS.length)]; //code duplication, was tempted to set on the squad
		}
	}

	private String getSmallEmblemName(Squad squad) {
		String smallEmblemName = squad.getSmallEmblemName();
		if (smallEmblemName != null) {
			return smallEmblemName;
		} else {
			return DEFAULT_SQUAD_EMBLEMS[(int) (squad.getId() % DEFAULT_SQUAD_EMBLEMS.length)] + "_small"; //code duplication, was tempted to set on the squad
		}
	}

	/**
	 * Updates every second or so, not instant on show
	 */
	@Override
	public void update() {
		for (Updatable<?> updatable : updatables) {
			updatable.update();
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

//Old Code
	/*

//		outerTable = new Table();
//		outerTable.pad(10);
//
//		shiftButton = i18nWidgetFactory.createTextButton("MILITARY.SQUAD.DAY_SHIFT_LABEL");
//		shiftButton.addListener(new ClickListener() {
//			@Override
//			public void clicked(InputEvent event, float x, float y) {
//				Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
//				if (squad != null) {
//					MilitaryShift newShift = squad.getShift().toggle();
//
//					squad.setShift(newShift);
//					messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_SHIFT_CHANGED, squad);
//					updateShiftButtonText(newShift);
//				}
//			}
//		});
//
//		trainingOrderButton = imageButtonFactory.getOrCreate("barracks").clone();
//		trainingOrderButton.setAction(() -> {
//			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
//			if (squad != null) {
//				messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_ORDERS_CHANGED, new SquadOrderChangeMessage(squad, SquadOrderType.TRAINING));
//				updateButtonToggle(squad);
//			}
//		});
//		guardOrderButton = imageButtonFactory.getOrCreate("move").clone();
//		guardOrderButton.setAction(() -> {
//			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
//			if (squad != null) {
//				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.SQUAD_MOVE_TO_LOCATION);
//				updateButtonToggle(squad);
//			}
//		});
//		attackOrderButton = imageButtonFactory.getOrCreate("crosshair-arrow").clone();
//		attackOrderButton.setAction(() -> {
//			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
//			if (squad != null) {
//				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.SQUAD_ATTACK_CREATURE);
//				updateButtonToggle(squad);
//			}
//		});
//		cancelAttackOrderButton = imageButtonFactory.getOrCreate("cancel").clone();
//		cancelAttackOrderButton.setAction(() -> {
//			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
//			if (squad != null) {
//				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.CANCEL_ATTACK_CREATURE);
//				updateButtonToggle(squad);
//			}
//		});
//		retreatOrderButton = imageButtonFactory.getOrCreate("run").clone();
//		retreatOrderButton.setAction(() -> {
//			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
//			if (squad != null) {
//				messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_ORDERS_CHANGED, new SquadOrderChangeMessage(squad, SquadOrderType.RETREATING));
//				updateButtonToggle(squad);
//			}
//		});
//
//		squadFormationSelectBox = new SaulSelectBox<>(uiSkin);
//		squadFormationSelectBox.setItems(orderedArray(squadFormationDictionary.getAll(), null));
//		squadFormationSelectBox.addListener(new ChangeListener() {
//			@Override
//			public void changed(ChangeEvent event, Actor actor) {
//				Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
//				if (squad != null) {
//					SquadFormation selectedFormation = squadFormationSelectBox.getSelected();
//					if (!selectedFormation.equals(squad.getFormation())) {
//						squad.setFormation(selectedFormation);
//					}
//				}
//			}
//		});
//
//		upperTable = new Table(uiSkin);
//		lowerTable = new Table(uiSkin);
	}

	@Override
	public void populate(Table containerTable) {
//		update();
//
//		containerTable.clear();
//
//		containerTable.add(outerTable);
	 */



	/*
	if (squadFormationSelectBox.isListDisplayed()) {
			// Don't refresh everything while list is open or it'll close
			return;
		}

		outerTable.clear();

		upperTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(SQUAD)) {
			Squad squad = selectable.getSquad();

			upperTable.add(new Label(squad.getName(), uiSkin)).left().pad(5).row();
			for (I18nText descriptionText : squad.getDescription(i18nTranslator, gameContext, messageDispatcher)) {
				upperTable.add(new I18nTextWidget(descriptionText, uiSkin, messageDispatcher)).left().pad(5).row();
			}
			updateShiftButtonText(squad.getShift());
			upperTable.add(shiftButton).left().pad(5).row();
			upperTable.add(new Label("FORMATION:", uiSkin)).left().pad(5).row();
			squadFormationSelectBox.setSelected(squad.getFormation());
			upperTable.add(squadFormationSelectBox).left().pad(5).row();


			updateButtonToggle(squad);
			for (ImageButton cursorButton : List.of(trainingOrderButton, guardOrderButton, attackOrderButton, cancelAttackOrderButton, retreatOrderButton)) {
				lowerTable.add(cursorButton).pad(5).left();
			}

			outerTable.add(upperTable).left().row();
			outerTable.add(lowerTable).left().row();
		}
	 */
//
//	private void updateShiftButtonText(MilitaryShift shift) {
//		shiftButton.setText(shift.getI18nKey(), i18nTranslator.getTranslatedString(shift.getI18nKey()).toString());
//	}
//
//	private void updateButtonToggle(Squad squad) {
//		for (ImageButton cursorButton : List.of(trainingOrderButton, guardOrderButton, attackOrderButton, cancelAttackOrderButton, retreatOrderButton)) {
//			cursorButton.setToggledOn(false);
//		}
//		switch (squad.getCurrentOrderType()) {
//			case TRAINING -> trainingOrderButton.setToggledOn(true);
//			case GUARDING -> guardOrderButton.setToggledOn(true);
//			case COMBAT -> attackOrderButton.setToggledOn(true);
//			case RETREATING -> retreatOrderButton.setToggledOn(true);
//		}
//	}
}
