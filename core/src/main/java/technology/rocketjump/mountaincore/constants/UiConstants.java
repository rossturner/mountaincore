package technology.rocketjump.mountaincore.constants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UiConstants {

	private String defaultFont;
	private String headerFont;
	private float cameraPanningSpeed;

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

	public float getCameraPanningSpeed() {
		return cameraPanningSpeed;
	}

	public void setCameraPanningSpeed(float cameraPanningSpeed) {
		this.cameraPanningSpeed = cameraPanningSpeed;
	}
}
