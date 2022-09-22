package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.underground.UnderTile;
import technology.rocketjump.saul.production.StockpileAllocation;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rooms.components.StockpileRoomComponent;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nTextWidget;
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;

import static technology.rocketjump.saul.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.saul.ui.Selectable.SelectableType.TILE;

@Singleton
public class TileSelectedGuiView implements GuiView {

	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final MessageDispatcher messageDispatcher;
	private Table descriptionTable;

	@Inject
	public TileSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
							   GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory) {
		uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;

		descriptionTable = new Table(uiSkin);
		descriptionTable.background("default-rect");
		descriptionTable.pad(10);
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();

		containerTable.add(descriptionTable);
	}

	@Override
	public void update() {
		descriptionTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(TILE)) {
			MapTile tile = selectable.getTile();
			if (tile != null) {
				if (tile.getExploration().equals(EXPLORED)) {
					descriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(tile), uiSkin, messageDispatcher)).left().row();

					UnderTile underTile = tile.getUnderTile();
					if (underTile != null) {
						Entity pipeEntity = underTile.getPipeEntity();
						if (pipeEntity != null) {
							descriptionTable.add(new I18nTextWidget(i18nTranslator.getPipeDescription(pipeEntity, underTile), uiSkin, messageDispatcher)).left().row();
						}
						Entity powerMechanismEntity = underTile.getPowerMechanismEntity();
						if (powerMechanismEntity != null) {
							descriptionTable.add(new I18nTextWidget(i18nTranslator.getPowerMechanismDescription(powerMechanismEntity, underTile), uiSkin, messageDispatcher)).left().row();
						}
					}
				} else {
					descriptionTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString("FLOOR.UNEXPLORED"), uiSkin, messageDispatcher)).left().row();
				}
				if (GlobalSettings.DEV_MODE) {
					if (tile.getRoomTile() != null) {
						StockpileRoomComponent stockpileRoomComponent = tile.getRoomTile().getRoom().getComponent(StockpileRoomComponent.class);
						if (stockpileRoomComponent != null) {
							StockpileAllocation stockpileAllocation = stockpileRoomComponent.getAllocationAt(tile.getTilePosition());
							if (stockpileAllocation == null) {
								descriptionTable.add(new Label("Stockpile allocations - null", uiSkin)).left().row();
							} else {
								descriptionTable.add(new Label("Stockpile allocations - Incoming: "+stockpileAllocation.getIncomingHaulingQuantity() +
										" In tile: " + stockpileAllocation.getQuantityInTile(), uiSkin)).left().row();

							}
						}

						descriptionTable.add(new Label("Room: " + tile.getRoomTile().getRoom(), uiSkin)).left().row();
					}


					descriptionTable.add(new Label("Location: " + tile.getTilePosition(), uiSkin)).left().row();
					descriptionTable.add(new Label("Roof: " + tile.getRoof().getState(), uiSkin)).left().row();
					descriptionTable.add(new Label("Region: " + tile.getRegionId(), uiSkin)).left().row();
					descriptionTable.add(new Label("Zones: " + StringUtils.join(tile.getZones(), ", "), uiSkin)).left().row();
					if (tile.getUnderTile() != null) {
						descriptionTable.add(new Label("UnderTile: " + tile.getUnderTile().toString(), uiSkin)).left().row();
					}
				}
			}
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.TILE_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

}
