package technology.rocketjump.saul.constants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UiConstants {

	private String defaultFont;
	private String headerFont;

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

}
