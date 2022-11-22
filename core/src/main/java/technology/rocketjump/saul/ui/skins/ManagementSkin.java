package technology.rocketjump.saul.ui.skins;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

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
}
