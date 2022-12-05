package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
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
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;

import static technology.rocketjump.saul.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.saul.ui.Selectable.SelectableType.TILE;

@Singleton
public class TileSelectedGuiView implements GuiView {

	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final MessageDispatcher messageDispatcher;
	private final Table headerContainer;
	private final Table topRow;
	private final Table mainTable;
	private final Label headerLabel;
	private final Label.LabelStyle labelStyle;
	private final Label.LabelStyle devModeLabelStyle;
	private final Table descriptionTable;

	@Inject
	public TileSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
							   GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory) {
		skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;

		mainTable = new Table(skin);
		mainTable.setTouchable(Touchable.enabled);
		mainTable.background("ENTITY_SELECT_BG_SMALL");
		mainTable.pad(20);

		descriptionTable = new Table();
		descriptionTable.defaults().padBottom(20);

		headerContainer = new Table();
		headerContainer.setBackground(skin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));
		headerLabel = new Label("", skin.get("title-header", Label.LabelStyle.class));
		headerContainer.add(headerLabel).center();

		topRow = new Table();
		topRow.add(new Container<>()).width(200);
		topRow.add(headerContainer).expandX();
		topRow.add(new Container<>()).width(200);

		labelStyle = skin.get("default-red", Label.LabelStyle.class);
		devModeLabelStyle = new Label.LabelStyle(labelStyle);
		devModeLabelStyle.fontColor = Color.PURPLE;
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();

		containerTable.add(mainTable).padLeft(20);
	}

	@Override
	public void update() {
		mainTable.clearChildren();
		descriptionTable.clearChildren();
		mainTable.add(topRow).expandX().fillX().top().row();
		mainTable.add(descriptionTable).expand().center().top().padTop(20).row();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(TILE)) {
			MapTile tile = selectable.getTile();
			if (tile != null) {
				if (tile.getExploration().equals(EXPLORED)) {
					headerLabel.setText(i18nTranslator.getDescription(tile).toString());

					UnderTile underTile = tile.getUnderTile();
					if (underTile != null) {
						Entity pipeEntity = underTile.getPipeEntity();
						if (pipeEntity != null) {
							descriptionTable.add(new Label(i18nTranslator.getPipeDescription(pipeEntity, underTile).toString(), labelStyle)).center().row();
						}
						Entity powerMechanismEntity = underTile.getPowerMechanismEntity();
						if (powerMechanismEntity != null) {
							descriptionTable.add(new Label(i18nTranslator.getPowerMechanismDescription(powerMechanismEntity, underTile).toString(), labelStyle)).center().row();
						}
					}
				} else {
					headerLabel.setText(i18nTranslator.getTranslatedString("FLOOR.UNEXPLORED").toString());
				}
				if (GlobalSettings.DEV_MODE) {
					if (tile.getRoomTile() != null) {
						StockpileRoomComponent stockpileRoomComponent = tile.getRoomTile().getRoom().getComponent(StockpileRoomComponent.class);
						if (stockpileRoomComponent != null) {
							StockpileAllocation stockpileAllocation = stockpileRoomComponent.getAllocationAt(tile.getTilePosition());
							if (stockpileAllocation == null) {
								descriptionTable.add(new Label("Stockpile allocations - null", devModeLabelStyle)).center().row();
							} else {
								descriptionTable.add(new Label("Stockpile allocations - Incoming: "+stockpileAllocation.getIncomingHaulingQuantity() +
										" In tile: " + stockpileAllocation.getQuantityInTile(), devModeLabelStyle)).center().row();

							}
						}

						descriptionTable.add(new Label("Room: " + tile.getRoomTile().getRoom(), devModeLabelStyle)).center().row();
					}


					descriptionTable.add(new Label("Location: " + tile.getTilePosition(), devModeLabelStyle)).center().row();
					descriptionTable.add(new Label("Roof: " + tile.getRoof().getState(), devModeLabelStyle)).center().row();
					descriptionTable.add(new Label("Region: " + tile.getRegionId(), devModeLabelStyle)).center().row();
					descriptionTable.add(new Label("Zones: " + StringUtils.join(tile.getZones(), ", "), devModeLabelStyle)).center().row();
					if (tile.getUnderTile() != null) {
						descriptionTable.add(new Label("UnderTile: " + tile.getUnderTile().toString(), devModeLabelStyle)).center().row();
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
