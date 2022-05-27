package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.doors.Doorway;
import technology.rocketjump.saul.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.*;

import static technology.rocketjump.saul.ui.Selectable.SelectableType.DOORWAY;

@Singleton
public class DoorwaySelectedGuiView implements GuiView {

	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final IconButton deconstructButton;
	private final MessageDispatcher messageDispatcher;
	private Table outerTable;
	private Table entityDescriptionTable;
	private Label beingDeconstructedLabel;

	@Inject
	public DoorwaySelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
								  GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory,
								  I18nWidgetFactory i18NWidgetFactory) {
		uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;

		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);

		entityDescriptionTable = new Table(uiSkin);
		entityDescriptionTable.pad(10);

		deconstructButton = iconButtonFactory.create("GUI.REMOVE_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.SMALL);
		final DoorwaySelectedGuiView This = this;
		deconstructButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(DOORWAY)) {
				Doorway doorway = selectable.getDoorway();
				Entity doorwayEntity = doorway.getDoorEntity();
				ConstructedEntityComponent constructedEntityComponent = doorwayEntity.getComponent(ConstructedEntityComponent.class);
				if (constructedEntityComponent != null && !constructedEntityComponent.isBeingDeconstructed()) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, doorwayEntity);
					This.update();
				}
			}
		});

		beingDeconstructedLabel = i18NWidgetFactory.createLabel("GUI.FURNITURE_BEING_REMOVED");
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
		entityDescriptionTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(DOORWAY)) {
			Doorway doorway = selectable.getDoorway();
			Entity entity = doorway.getDoorEntity();
			entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(entity), uiSkin, messageDispatcher)).left();
			entityDescriptionTable.row();

			outerTable.add(entityDescriptionTable);

			ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
			if (constructedEntityComponent != null) {
				if (constructedEntityComponent.isBeingDeconstructed()) {
					outerTable.add(beingDeconstructedLabel);
				} else {
					outerTable.add(deconstructButton);
				}
			}
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.DOORWAY_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

}
