package technology.rocketjump.saul.ui.skins;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;

public class ManagementSkin extends Skin {

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
}
