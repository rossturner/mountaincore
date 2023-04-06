package technology.rocketjump.mountaincore.ui.widgets.libgdxclone;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import technology.rocketjump.mountaincore.misc.SteamUtils;

public class MountaincoreTextField extends TextField {

	public MountaincoreTextField(String text, Skin skin) {
		super(text, skin);
		if (SteamUtils.isRunningOnSteamDeck()) {
			this.setOnscreenKeyboard(new SteamDeckKeyboard(this));
		}
	}

	public MountaincoreTextField(String text, Skin skin, String styleName) {
		super(text, skin, styleName);
		if (SteamUtils.isRunningOnSteamDeck()) {
			this.setOnscreenKeyboard(new SteamDeckKeyboard(this));
		}
	}

	public MountaincoreTextField(String text, TextFieldStyle style) {
		super(text, style);
		if (SteamUtils.isRunningOnSteamDeck()) {
			this.setOnscreenKeyboard(new SteamDeckKeyboard(this));
		}
	}

}
