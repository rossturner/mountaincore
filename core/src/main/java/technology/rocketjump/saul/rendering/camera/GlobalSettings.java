package technology.rocketjump.saul.rendering.camera;

import com.google.common.io.Resources;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.misc.versioning.Version;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

public class GlobalSettings {
	public static final boolean MAP_REVEALED = false;
	public static boolean DEV_MODE = true;
	public static final boolean CHOOSE_SPAWN_LOCATION = true;
	public static final boolean UI_DEBUG = true;

	public static boolean USE_EDGE_SCROLLING = true;
	public static boolean ZOOM_TO_CURSOR = true;
	public static boolean TREE_TRANSPARENCY_ENABLED = true;
	public static final Version VERSION;
	public static boolean STRESS_TEST = false;

	static {
		String loadedVersion = "UNKNOWN 0";
		try {
			URL resourceUrl = Resources.getResource("version.txt");
			loadedVersion = Resources.toString(resourceUrl, Charset.defaultCharset());
		} catch (IOException e) {
			Logger.error(e);
		} finally {
			VERSION = new Version(loadedVersion);
		}
	}
}
