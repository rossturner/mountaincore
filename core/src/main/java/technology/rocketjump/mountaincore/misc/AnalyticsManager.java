package technology.rocketjump.mountaincore.misc;

import com.badlogic.gdx.Gdx;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnalyticsManager {

	private static final long PERIOD_IN_SECONDS = 60;

	private static final GoogleAnalytics ga;
	private static String clientId = "Unknown";
	public static String languageCode = "en-gb";
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "AnalyticsThread");
		t.setDaemon(true);
		return t;
	});

	static {
		ga = GoogleAnalytics.builder()
				.withTrackingId("UA-82195631-5")
				.build();
	}

	public static void startAnalytics(String clientId) {
		AnalyticsManager.clientId = clientId;
		executor.scheduleAtFixedRate(AnalyticsManager::postAnalyticsInfo, 0, PERIOD_IN_SECONDS, TimeUnit.SECONDS);
	}

	public static void stopAnalytics() {
		executor.shutdownNow();
	}

	private static void postAnalyticsInfo() {
		try {
			ga.pageView("http://client.kingunderthemounta.in", "Main")
					.applicationName("King under the Mountain")
					.applicationVersion(GlobalSettings.VERSION.toString())
					.clientId(clientId)
					.screenResolution(Gdx.graphics.getDisplayMode(Gdx.graphics.getMonitor()).toString())
					.userLanguage(languageCode)
					.send();
		} catch (Exception e) {
			// Suppress any tracking-related exceptions outside of dev mode
			if (GlobalSettings.DEV_MODE) {
				Logger.error(e);
			}
		}
	}

}
