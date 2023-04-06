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
				Vector2 screenPosition = parent.localToScreenCoordinates(new Vector2(0, 0));

				parent.setSelection(0, parent.getText().length());
				if ((int)screenPosition.y > Gdx.graphics.getHeight() / 2) {
					steamUtils.showFloatingGamepadTextInput(SteamUtils.FloatingGamepadTextInputMode.ModeSingleLine,
							(int) screenPosition.x, Gdx.graphics.getHeight() - (int) screenPosition.y, 10, 10);
				} else {
					steamUtils.showFloatingGamepadTextInput(SteamUtils.FloatingGamepadTextInputMode.ModeSingleLine,
							0, 0, 10, 10);
				}
			}
		}
	}
}
