package technology.rocketjump.saul.constants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UiConstants {

	private String defaultFont;
	private String headerFont;
	private float defaultFontScale = 2.0f;
	private float headerFontScale = 2.0f;

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
}
