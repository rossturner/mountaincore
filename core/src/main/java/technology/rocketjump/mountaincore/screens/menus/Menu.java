package technology.rocketjump.mountaincore.screens.menus;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

public interface Menu {

	void show();

	void hide();

	void populate(Table containerTable);

	void reset();

	default boolean showVersionDetails() {
		return false;
	}
}
