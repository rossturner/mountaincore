package technology.rocketjump.saul.screens.menus.options;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

public interface OptionsTab {

	void populate(Table menuTable);

	OptionsTabName getTabName();

}
