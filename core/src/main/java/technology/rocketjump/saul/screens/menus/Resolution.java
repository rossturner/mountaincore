package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.utils.Array;

public class Resolution {

	public static final Array<Resolution> defaultResolutions = new Array<>();

	static {
		defaultResolutions.addAll(
				new Resolution(1280, 720),
				new Resolution(1280, 800),
				new Resolution(1280, 1024),
				new Resolution(1360, 768),
				new Resolution(1366, 768),
				new Resolution(1440, 900),
				new Resolution(1600, 900),
				new Resolution(1680, 1050),
				new Resolution(1600, 1200),
				new Resolution(1920, 1080),
				new Resolution(1920, 1200),
				new Resolution(2560, 1080),
				new Resolution(2560, 1440),
				new Resolution(2560, 1600),
				new Resolution(3440, 1440),
				new Resolution(3840, 2160)
		);
	}

	public final int width;
	public final int height;

	public Resolution(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return width + "x" + height;
	}

	public static Resolution byString(String stringRepresentation) {
		String[] parts = stringRepresentation.split("x");
		return new Resolution(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
	}

}
