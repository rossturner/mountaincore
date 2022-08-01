package technology.rocketjump.saul.assets.editor.model;

import com.badlogic.gdx.graphics.Color;

public class ColorPickerMessage {

	public final String initialHexCode;
	public final ColorPickerCallback callback;

	public ColorPickerMessage(String initialHexCode, ColorPickerCallback callback) {
		this.initialHexCode = initialHexCode;
		this.callback = callback;
	}

	public interface ColorPickerCallback {

		void colorPicked(Color color);

	}
}
