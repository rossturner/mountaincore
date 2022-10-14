package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class ScaledToFitLabel extends Label {

	private final float maxWidth;

	public ScaledToFitLabel(CharSequence text, Skin skin, float maxWidth) {
		super(text, skin);
		this.maxWidth = maxWidth;
		scaleToFit();
	}

	public ScaledToFitLabel(CharSequence text, Skin skin, String styleName, float maxWidth) {
		super(text, skin, styleName);
		this.maxWidth = maxWidth;
		scaleToFit();
	}

	public ScaledToFitLabel(CharSequence text, Skin skin, String fontName, Color color, float maxWidth) {
		super(text, skin, fontName, color);
		this.maxWidth = maxWidth;
		scaleToFit();
	}

	public ScaledToFitLabel(CharSequence text, Skin skin, String fontName, String colorName, float maxWidth) {
		super(text, skin, fontName, colorName);
		this.maxWidth = maxWidth;
		scaleToFit();
	}

	public ScaledToFitLabel(CharSequence text, LabelStyle style, float maxWidth) {
		super(text, style);
		this.maxWidth = maxWidth;
		scaleToFit();
	}

	@Override
	public boolean setText(int value) {
		boolean result = super.setText(value);
		scaleToFit();
		return result;
	}

	@Override
	public void setText(CharSequence newText) {
		super.setText(newText);
		scaleToFit();
	}

	private void scaleToFit() {
		setFontScale(1);
		layout();
		float glyphWidth = getGlyphLayout().width;

		float targetWidth = maxWidth * 0.92f;

		if (glyphWidth >= targetWidth) {
			float increase = (glyphWidth - targetWidth) / glyphWidth;
			setFontScale(1 - increase);
		}
	}
}
