package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.SquadOrderChangeMessage;
import technology.rocketjump.saul.military.model.MilitaryShift;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.military.model.SquadOrderType;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nTextButton;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.saul.ui.widgets.ImageButton;
import technology.rocketjump.saul.ui.widgets.ImageButtonFactory;

import java.util.List;

import static technology.rocketjump.saul.ui.Selectable.SelectableType.SQUAD;

@Singleton
public class SquadSelectedGuiView implements GuiView {

	private final Table outerTable;
	private final Table upperTable;
	private final Table lowerTable;
	private final Skin uiSkin;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final I18nTextButton shiftButton;
	private final I18nTranslator i18nTranslator;

	private final ImageButton trainingOrderButton;
	private final ImageButton guardOrderButton;
	private final ImageButton attackOrderButton;
	private final ImageButton cancelAttackOrderButton;
	private final ImageButton retreatOrderButton;

	@Inject
	public SquadSelectedGuiView(GuiSkinRepository guiSkinRepository, GameInteractionStateContainer gameInteractionStateContainer,
								I18nWidgetFactory i18nWidgetFactory, I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher,
								ImageButtonFactory imageButtonFactory) {
		this.uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;

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
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.REMOVE_DESIGNATIONS);
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
		outerTable.clear();

		upperTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(SQUAD)) {
			Squad squad = selectable.getSquad();

			upperTable.add(new Label(squad.getName(), uiSkin)).left().pad(5).row();
			upperTable.add(new Label("TODO: Description of what squad is doing", uiSkin)).left().pad(5).row();
			updateShiftButtonText(squad.getShift());
			upperTable.add(shiftButton).left().pad(5).row();
			upperTable.add(new Label("TODO: Formations", uiSkin)).left().pad(5).row();


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
}
