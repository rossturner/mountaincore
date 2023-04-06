package technology.rocketjump.mountaincore.ui.widgets.libgdxclone;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.codedisaster.steamworks.SteamUtils;

public class SteamDeckKeyboard implements TextField.OnscreenKeyboard {

	private final SteamUtils steamUtils;
	private final TextField parent;

	public SteamDeckKeyboard(TextField parent) {
		this.steamUtils = new SteamUtils(() -> {});
		this.parent = parent;
	}

	@Override
	public void show(boolean visible) {
		steamUtils.dismissFloatingGamepadTextInput();
		if (visible) {
			if (parent.getStage() != null) {
				Vector2 screenPosition = parent.getStage().stageToScreenCoordinates(new Vector2(parent.getX(), parent.getY()));
				Vector2 screenSize = parent.getStage().stageToScreenCoordinates(new Vector2(parent.getWidth(), parent.getHeight()));

				parent.setSelection(0, parent.getText().length());
				if ((int)screenPosition.y > Gdx.graphics.getHeight() / 2) {
					steamUtils.showFloatingGamepadTextInput(SteamUtils.FloatingGamepadTextInputMode.ModeSingleLine,
							(int) screenPosition.x, Gdx.graphics.getHeight() - (int) screenPosition.y, (int) screenSize.x, (int) screenSize.y);
				} else {
					steamUtils.showFloatingGamepadTextInput(SteamUtils.FloatingGamepadTextInputMode.ModeSingleLine,
							0, 0, 10, 10);
				}
			}
		}
	}
}
