package technology.rocketjump.mountaincore.constants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UiConstants {

	private String defaultFont;
	private String headerFont;
	private float cameraPanningSpeed;
	private float cameraMouseDragPanningSpeed;

	private Map<String, String> roofStateColors = new HashMap<>();

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

	public float getCameraMouseDragPanningSpeed() {
		return cameraMouseDragPanningSpeed;
	}

	public void setCameraMouseDragPanningSpeed(float cameraMouseDragPanningSpeed) {
		this.cameraMouseDragPanningSpeed = cameraMouseDragPanningSpeed;
	}

	public Map<String, String> getRoofStateColors() {
		return roofStateColors;
	}

	public void setRoofStateColors(Map<String, String> roofStateColors) {
		this.roofStateColors = roofStateColors;
	}
}
