package technology.rocketjump.mountaincore.ui.skins;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemQuality;
import technology.rocketjump.mountaincore.military.model.Squad;

public class ManagementSkin extends Skin {

	public static final String[] DEFAULT_SQUAD_EMBLEMS = {
			"icon_military_emblem_hammer",
			"icon_military_emblem_snow",
			"icon_military_emblem_wolf",
			"icon_military_emblem_tree",
			"icon_military_emblem_helmet",
			"icon_military_emblem_fire",
			"icon_military_emblem_arrow",
			"icon_military_emblem_skull"
	};
	private Drawable[] btnResourceItemVariants;


	public Drawable bgForExampleEntity(long entityId) {
		if (btnResourceItemVariants == null) {
			btnResourceItemVariants = new Drawable[]{
				getDrawable("btn_resources_item_01"),
				getDrawable("btn_resources_item_02"),
				getDrawable("btn_resources_item_03"),
				getDrawable("btn_resources_item_04")
			};
		}

		return btnResourceItemVariants[(int) (entityId % btnResourceItemVariants.length)];
	}

	public Drawable getQualityDrawableForCorner(ItemQuality itemQuality) {
		String drawableName = switch (itemQuality) {
			case AWFUL -> "_0000_asset_quality_star_01";
			case POOR -> "_0001_asset_quality_star_02";
			case STANDARD -> "_0002_asset_quality_star_03";
			case SUPERIOR -> "_0003_asset_quality_star_04";
			case MASTERWORK -> "_0004_asset_quality_star_05";
		};
		return getDrawable(drawableName);
	}

	public String getEmblemName(Squad squad) {
		String emblemName = squad.getEmblemName();
		if (emblemName != null) {
			return emblemName;
		} else {
			return DEFAULT_SQUAD_EMBLEMS[(int) ((squad.getId() - 1) % DEFAULT_SQUAD_EMBLEMS.length)]; //code duplication, was tempted to set on the squad
		}
	}

	public String getSmallEmblemName(Squad squad) {
		String smallEmblemName = squad.getSmallEmblemName();
		if (smallEmblemName != null) {
			return smallEmblemName;
		} else {
			return DEFAULT_SQUAD_EMBLEMS[(int) ((squad.getId() - 1) % DEFAULT_SQUAD_EMBLEMS.length)] + "_small"; //code duplication, was tempted to set on the squad
		}
	}
}
