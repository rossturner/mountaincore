package technology.rocketjump.saul.constants;

import com.badlogic.gdx.math.Vector2;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UiConstants {

	private final static Vector2 VIEWPORT_DIMENSIONS = new Vector2(3840, 2160);

	private String defaultFont;
	private String headerFont;
	private float defaultFontScale = 2.0f;
	private float headerFontScale = 2.0f;
	private float viewportScale = 1.0f;

	public String getDefaultFont() {
		return defaultFont;
	}

	public void setDefaultFont(String defaultFont) {
		this.defaultFont = defaultFont;
	}

	public String getHeaderFont() {
		return headerFont;
	}

	public void setHeaderFont(String headerFont) {
		this.headerFont = headerFont;
	}

	public float getDefaultFontScale() {
		return defaultFontScale;
	}

	public void setDefaultFontScale(float defaultFontScale) {
		this.defaultFontScale = defaultFontScale;
	}

	public float getHeaderFontScale() {
		return headerFontScale;
	}

	public void setHeaderFontScale(float headerFontScale) {
		this.headerFontScale = headerFontScale;
	}

	public float getViewportScale() {
		return viewportScale;
	}

	public void setViewportScale(float viewportScale) {
		this.viewportScale = viewportScale;
	}

	public Vector2 calculateViewportDimensions() {
		return VIEWPORT_DIMENSIONS.cpy().scl(viewportScale);
	}
}
