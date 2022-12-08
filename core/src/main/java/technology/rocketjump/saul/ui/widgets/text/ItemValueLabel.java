package technology.rocketjump.saul.ui.widgets.text;

import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class ItemValueLabel extends HorizontalGroup {

	public ItemValueLabel(int value, String labelStyleName, Skin skin) {
		Label.LabelStyle labelStyle = skin.get(labelStyleName, Label.LabelStyle.class);
		this.space(labelStyle.font.getSpaceXadvance());

		/**
		 * Hard-coded to dividing by 10 for gold and silver values, should be driven by moddable SettlementConstants but okay for now
		 */
		int goldValue = value / 10;
		int silverValue = (value % 10);

		if (goldValue > 0) {
			addActor(new Label(String.valueOf(goldValue), labelStyle));
			addActor(new Image(skin, "icon_coin"));
		}
		if (silverValue != 0 || goldValue == 0) {
			addActor(new Label(String.valueOf(silverValue), labelStyle));
			addActor(new Image(skin, "icon_coin_greyscale"));
		}
	}

}
