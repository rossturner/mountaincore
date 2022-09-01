package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.SquadOrderChangeMessage;
import technology.rocketjump.saul.military.SquadFormationDictionary;
import technology.rocketjump.saul.military.model.MilitaryShift;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.military.model.SquadOrderType;
import technology.rocketjump.saul.military.model.formations.SquadFormation;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.*;
import technology.rocketjump.saul.ui.widgets.libgdxclone.SaulSelectBox;

import java.util.List;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;
import static technology.rocketjump.saul.ui.Selectable.SelectableType.SQUAD;

@Singleton
public class SquadSelectedGuiView implements GuiView, GameContextAware {

	private final Table outerTable;
	private final Table upperTable;
	private final Table lowerTable;
	private final Skin uiSkin;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final I18nTextButton shiftButton;
	private final I18nTranslator i18nTranslator;

	private final SaulSelectBox<SquadFormation> squadFormationSelectBox;

	private final ImageButton trainingOrderButton;
	private final ImageButton guardOrderButton;
	private final ImageButton attackOrderButton;
	private final ImageButton cancelAttackOrderButton;
	private final ImageButton retreatOrderButton;
	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	@Inject
	public SquadSelectedGuiView(GuiSkinRepository guiSkinRepository, GameInteractionStateContainer gameInteractionStateContainer,
								I18nWidgetFactory i18nWidgetFactory, I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher,
								ImageButtonFactory imageButtonFactory, SquadFormationDictionary squadFormationDictionary,
								MessageDispatcher messageDispatcher1) {
		this.uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.messageDispatcher = messageDispatcher1;

		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);

		shiftButton = i18nWidgetFactory.createTextButton("MILITARY.SQUAD.DAY_SHIFT_LABEL");
		shiftButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
				if (squad != null) {
					MilitaryShift newShift = squad.getShift().toggle();

					squad.setShift(newShift);
					messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_SHIFT_CHANGED, squad);
					updateShiftButtonText(newShift);
				}
			}
		});

		trainingOrderButton = imageButtonFactory.getOrCreate("barracks").clone();
		trainingOrderButton.setAction(() -> {
			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
			if (squad != null) {
				messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_ORDERS_CHANGED, new SquadOrderChangeMessage(squad, SquadOrderType.TRAINING));
				updateButtonToggle(squad);
			}
		});
		guardOrderButton = imageButtonFactory.getOrCreate("move").clone();
		guardOrderButton.setAction(() -> {
			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
			if (squad != null) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.SQUAD_MOVE_TO_LOCATION);
				updateButtonToggle(squad);
			}
		});
		attackOrderButton = imageButtonFactory.getOrCreate("crosshair-arrow").clone();
		attackOrderButton.setAction(() -> {
			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
			if (squad != null) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.SQUAD_ATTACK_CREATURE);
				updateButtonToggle(squad);
			}
		});
		cancelAttackOrderButton = imageButtonFactory.getOrCreate("cancel").clone();
		cancelAttackOrderButton.setAction(() -> {
			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
			if (squad != null) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.CANCEL_ATTACK_CREATURE);
				updateButtonToggle(squad);
			}
		});
		retreatOrderButton = imageButtonFactory.getOrCreate("run").clone();
		retreatOrderButton.setAction(() -> {
			Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
			if (squad != null) {
				messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_ORDERS_CHANGED, new SquadOrderChangeMessage(squad, SquadOrderType.RETREATING));
				updateButtonToggle(squad);
			}
		});

		squadFormationSelectBox = new SaulSelectBox<>(uiSkin);
		squadFormationSelectBox.setItems(orderedArray(squadFormationDictionary.getAll(), null));
		squadFormationSelectBox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Squad squad = gameInteractionStateContainer.getSelectable().getSquad();
				if (squad != null) {
					SquadFormation selectedFormation = squadFormationSelectBox.getSelected();
					if (!selectedFormation.equals(squad.getFormation())) {
						squad.setFormation(selectedFormation);
					}
				}
			}
		});

		upperTable = new Table(uiSkin);
		lowerTable = new Table(uiSkin);
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();

		containerTable.add(outerTable);
	}

	@Override
	public void update() {
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
			for (I18nText descriptionText : squad.getDescription(i18nTranslator, gameContext)) {
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
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.SQUAD_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	private void updateShiftButtonText(MilitaryShift shift) {
		shiftButton.setText(shift.getI18nKey(), i18nTranslator.getTranslatedString(shift.getI18nKey()).toString());
	}

	private void updateButtonToggle(Squad squad) {
		for (ImageButton cursorButton : List.of(trainingOrderButton, guardOrderButton, attackOrderButton, cancelAttackOrderButton, retreatOrderButton)) {
			cursorButton.setToggledOn(false);
		}
		switch (squad.getCurrentOrderType()) {
			case TRAINING -> trainingOrderButton.setToggledOn(true);
			case GUARDING -> guardOrderButton.setToggledOn(true);
			case COMBAT -> attackOrderButton.setToggledOn(true);
			case RETREATING -> retreatOrderButton.setToggledOn(true);
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
