package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.underground.UnderTile;
import technology.rocketjump.mountaincore.production.StockpileAllocation;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.rooms.components.StockpileRoomComponent;
import technology.rocketjump.mountaincore.ui.GameInteractionStateContainer;
import technology.rocketjump.mountaincore.ui.Selectable;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;

import static technology.rocketjump.mountaincore.mapping.tile.TileExploration.EXPLORED;

@Singleton
public class TileSelectedGuiView implements GuiView {

	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final MessageDispatcher messageDispatcher;
	private final Table headerContainer;
	private final Table topRow;
	private final Table mainTable;
	private final Table descriptionTable;

	@Inject
	public TileSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
							   GameInteractionStateContainer gameInteractionStateContainer) {
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

		topRow = new Table();
		topRow.add(new Container<>()).width(200);
		topRow.add(headerContainer).expandX();
		topRow.add(new Container<>()).width(200);
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
		headerContainer.clearChildren();

		Label headerLabel = new Label("", skin.get("title-header", Label.LabelStyle.class));
		headerContainer.add(headerLabel).center();


		mainTable.add(topRow).expandX().fillX().top().row();
		mainTable.add(descriptionTable).expand().center().top().padTop(20).row();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(Selectable.SelectableType.TILE)) {
			MapTile tile = selectable.getTile();
			if (tile != null) {
				if (tile.getExploration().equals(EXPLORED)) {
					headerLabel.setText(i18nTranslator.getDescription(tile).toString());

					UnderTile underTile = tile.getUnderTile();
					if (underTile != null) {
						Entity pipeEntity = underTile.getPipeEntity();
						if (pipeEntity != null) {
							descriptionTable.add(new Label(i18nTranslator.getPipeDescription(pipeEntity, underTile).toString(), skin, "default-red")).center().row();
						}
						Entity powerMechanismEntity = underTile.getPowerMechanismEntity();
						if (powerMechanismEntity != null) {
							descriptionTable.add(new Label(i18nTranslator.getPowerMechanismDescription(powerMechanismEntity, underTile).toString(), skin, "default-red")).center().row();
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
								descriptionTable.add(new Label("Stockpile allocations - null", skin, "debug-label")).center().row();
							} else {
								descriptionTable.add(new Label("Stockpile allocations - Incoming: "+stockpileAllocation.getIncomingHaulingQuantity() +
										" In tile: " + stockpileAllocation.getQuantityInTile(),  skin, "debug-label")).center().row();

							}
						}

						descriptionTable.add(new Label("Room: " + tile.getRoomTile().getRoom(),  skin, "debug-label")).center().row();
					}


					descriptionTable.add(new Label("Location: " + tile.getTilePosition(),  skin, "debug-label")).center().row();
					descriptionTable.add(new Label("Roof: " + tile.getRoof().getState(),  skin, "debug-label")).center().row();
					descriptionTable.add(new Label("Region: " + tile.getRegionId(),  skin, "debug-label")).center().row();
					descriptionTable.add(new Label("Zones: " + StringUtils.join(tile.getZones(), ", "),  skin, "debug-label")).center().row();
					if (tile.getUnderTile() != null) {
						descriptionTable.add(new Label("UnderTile: " + tile.getUnderTile().toString(),  skin, "debug-label")).center().row();
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
