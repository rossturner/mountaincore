package technology.rocketjump.saul.ui.widgets.furniture;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.behaviour.furniture.ProductionImportFurnitureBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.views.RoomEditorItemMap;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;

@Singleton
public class ProductionImportFurnitureWidget extends Table implements DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final RoomEditorItemMap roomEditorItemMap;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final EntityRenderer entityRenderer;

	private final MainGameSkin skin;
	private final Container<Button> buttonContainer = new Container<>();
	private final Drawable noneSelectedDrawable;
	private final Drawable backgroundDrawable;
	private Entity furnitureEntity;
	private ProductionImportFurnitureBehaviour productionImportBehaviour;


	@Inject
	public ProductionImportFurnitureWidget(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
										   TooltipFactory tooltipFactory, RoomEditorItemMap roomEditorItemMap,
										   GameMaterialDictionary gameMaterialDictionary, EntityRenderer entityRenderer) {
		this.messageDispatcher = messageDispatcher;
		this.skin = guiSkinRepository.getMainGameSkin();
		this.tooltipFactory = tooltipFactory;
		this.roomEditorItemMap = roomEditorItemMap;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.entityRenderer = entityRenderer;

		backgroundDrawable = skin.getDrawable("asset_bg");
		buttonContainer.setBackground(backgroundDrawable);

		noneSelectedDrawable = skin.getDrawable("icon_not_equipped_no_bg");
	}

	public void setFurnitureEntity(Entity entity) {
		if (entity.getBehaviourComponent() instanceof ProductionImportFurnitureBehaviour productionImportFurnitureBehaviour) {
			this.furnitureEntity = entity;
			this.productionImportBehaviour = productionImportFurnitureBehaviour;
			rebuildUI();
		} else {
			Logger.error("Entity {} passed to {} is not a {}", entity, getClass().getSimpleName(), ProductionImportFurnitureBehaviour.class.getSimpleName());
			clearChildren();
		}
	}

	@Override
	public void rebuildUI() {
		clearChildren();
		if (furnitureEntity == null || productionImportBehaviour == null) {
			return;
		}

		ItemType selectedItemType = productionImportBehaviour.getSelectedItemType();

		Button button;
		if (selectedItemType == null) {
			button = new Button(noneSelectedDrawable);
			tooltipFactory.simpleTooltip(button, "SOMETHING", TooltipLocationHint.ABOVE);
		} else {
			Entity displayedEntity = roomEditorItemMap.getByItemType(selectedItemType);
			GameMaterial selectedMaterial = productionImportBehaviour.getSelectedMaterial();
			if (selectedMaterial == null) {
				selectedMaterial = gameMaterialDictionary.getExampleMaterial(selectedItemType.getPrimaryMaterialType());
			}
			((ItemEntityAttributes) displayedEntity.getPhysicalEntityComponent().getAttributes()).setMaterial(selectedMaterial);
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, displayedEntity);

			EntityDrawable entityDrawable = new EntityDrawable(displayedEntity, entityRenderer, true, messageDispatcher);
			entityDrawable.setMinSize(backgroundDrawable.getMinWidth(), backgroundDrawable.getMinHeight());
			button = new Button(entityDrawable);
			tooltipFactory.simpleTooltip(buttonContainer, selectedItemType.getI18nKey(), TooltipLocationHint.ABOVE);
		}

		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ProductionImportFurnitureWidget.this.onClick();
			}
		});
		button.addListener(new ChangeCursorOnHover(buttonContainer, GameCursor.SELECT, messageDispatcher));
		buttonContainer.setActor(button);

		this.add(buttonContainer).center().row();

		// TODO material selection

	}

	private void onClick() {
		Logger.info("TODO");
	}

}
