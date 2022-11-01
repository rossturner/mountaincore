package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.settlement.ItemAvailabilityChecker;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FurnitureRequirementWidget extends Table {

	private final QuantifiedItemType requirement;
	private final MessageDispatcher messageDispatcher;
	private final ItemAvailabilityChecker itemAvailabilityChecker;
	private final I18nTranslator i18nTranslator;

	private final List<GameMaterial> materials;
	private final Entity itemEntity;
	private final GameMaterial defaultDisplayMaterial;
	private final Skin skin;
	private final Button leftButton, rightButton;
	private final Image entityImage;
	private int selectionIndex;

	private Label label;

	private Consumer<GameMaterial> callback;

	public FurnitureRequirementWidget(QuantifiedItemType requirement, Entity itemEntity, Skin skin, MessageDispatcher messageDispatcher,
									  ItemAvailabilityChecker itemAvailabilityChecker, I18nTranslator i18nTranslator, EntityRenderer entityRenderer,
									  TooltipFactory tooltipFactory, GameMaterial defaultDisplayMaterial) {
		this.requirement = requirement;
		this.skin = skin;
		this.messageDispatcher = messageDispatcher;
		this.itemAvailabilityChecker = itemAvailabilityChecker;
		this.i18nTranslator = i18nTranslator;
		this.itemEntity = itemEntity;
		this.defaultDisplayMaterial = defaultDisplayMaterial;

		this.setDebug(GlobalSettings.UI_DEBUG);
		this.defaults().pad(4);

		materials = new ArrayList<>(itemAvailabilityChecker.getAvailableMaterialsFor(requirement.getItemType(), requirement.getQuantity()));
		materials.add(0, null);

		updateEntity();

		leftButton = new Button(skin.get("btn_arrow_small_left", Button.ButtonStyle.class));
		if (materials.size() > 1) {
			leftButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					changeSelectionIndex(-1);
				}
			});
			leftButton.addListener(new ChangeCursorOnHover(leftButton, GameCursor.SELECT, messageDispatcher));
		} else {
			leftButton.setDisabled(true);
		}
		rightButton = new Button(skin.get("btn_arrow_small_right", Button.ButtonStyle.class));
		if (materials.size() > 1) {
			rightButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					changeSelectionIndex(1);
				}
			});
			rightButton.addListener(new ChangeCursorOnHover(rightButton, GameCursor.SELECT, messageDispatcher));
		} else {
			rightButton.setDisabled(true);
		}
		entityImage = new Image(new EntityDrawable(itemEntity, entityRenderer, true, messageDispatcher));
		tooltipFactory.simpleTooltip(entityImage, requirement.getItemType().getI18nKey(), TooltipLocationHint.ABOVE);

		resetLabel();
	}

	private void rebuildUI() {
		this.clearChildren();
		this.add(leftButton);
		this.add(entityImage).size(100, 100);
		this.add(rightButton).row();
		this.add(label).colspan(3).expandX().center().row();
	}

	private void changeSelectionIndex(int amount) {
		selectionIndex += amount;
		if (selectionIndex < 0) {
			selectionIndex = materials.size() - 1;
		}
		if (selectionIndex >= materials.size()) {
			selectionIndex = 0;
		}
		if (callback != null) {
			callback.accept(materials.get(selectionIndex));
		}
		updateEntity();
		resetLabel();
	}

	private void updateEntity() {
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		attributes.setQuantity(requirement.getQuantity());

		GameMaterial selected = materials.get(selectionIndex);
		if (selected == null) {
			selected = defaultDisplayMaterial;
		}
		attributes.setMaterial(selected);

		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemEntity);
	}

	private void resetLabel() {
		GameMaterial selected = materials.get(selectionIndex);
		String text;
		if (selected == null) {
			text = i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY.LABEL").toString();
		} else {
			text = i18nTranslator.getTranslatedString(selected.getI18nKey()).toString();
		}
		label = new Label(text, skin.get("default-red", Label.LabelStyle.class));

		rebuildUI();
	}

	public void onMaterialSelection(Consumer<GameMaterial> callback) {
		this.callback = callback;
	}
}
