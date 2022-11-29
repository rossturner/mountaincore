package technology.rocketjump.saul.logging;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.UserPreferences;

import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.CRASH_REPORTING;
import static technology.rocketjump.saul.rendering.camera.GlobalSettings.VERSION;

@Singleton
public class CrashHandler implements Telegraph {

	private static boolean reportingEnabled;
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final String CRASH_LOG_URL = "https://saul-api.herokuapp.com/api/crash";
//	private static final String CRASH_LOG_URL = "http://localhost:8080/api/crash";

	private final UserPreferences userPreferences;

	@Inject
	public CrashHandler(UserPreferences userPreferences, MessageDispatcher messageDispatcher) {
		this.userPreferences = userPreferences;
		reportingEnabled = Boolean.valueOf(userPreferences.getPreference(CRASH_REPORTING, "true"));
		messageDispatcher.addListener(this, MessageType.CRASH_REPORTING_OPT_IN_MODIFIED);
	}

	public boolean isOptInConfirmationRequired() {
		return userPreferences.getPreference(CRASH_REPORTING, null) == null;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.CRASH_REPORTING_OPT_IN_MODIFIED: {
				Boolean reportingEnabled = (Boolean) msg.extraInfo;
				userPreferences.setPreference(CRASH_REPORTING, String.valueOf(reportingEnabled));
				CrashHandler.reportingEnabled = reportingEnabled;
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public static void logCrash(Throwable exception) {
		Logger.error(exception);
		if (!reportingEnabled) {
			return;
		}
		try {
			OkHttpClient client = new OkHttpClient();
			JSONObject payload = new JSONObject();
			payload.put("gameVersion", VERSION.toString());
			payload.put("displaySettings", Gdx.graphics.getDisplayMode(Gdx.graphics.getMonitor()).toString());
			payload.put("stackTrace", ExceptionUtils.getStackTrace(exception));
			payload.put("preferencesJson", removeKeybindings(UserPreferences.preferencesJson));

			RequestBody body = RequestBody.create(JSON, payload.toJSONString());
			Request request = new Request.Builder()
					.url(CRASH_LOG_URL)
					.post(body)
					.build();
			Response response = client.newCall(request).execute();
			System.out.println("Response: " + response.code());
			IOUtils.closeQuietly(response);
		} catch (Exception e) {
			Logger.error("Failed to post expanded crash data: " + e.getMessage());
		}
	}

	private static String removeKeybindings(String preferencesJson) {
		JSONObject preferences = JSONObject.parseObject(preferencesJson);
		JSONObject filtered = new JSONObject(true);
		for (String key : preferences.keySet()) {
			if (EnumUtils.getEnum(UserPreferences.PreferenceKey.class, key) != null) {
				filtered.put(key, preferences.getString(key));
			}
		}
		return filtered.toJSONString();
	}

	private static String getGraphicsInfo() {
		return Gdx.gl20.glGetString(GL_VENDOR) + " " + Gdx.gl20.glGetString(GL_RENDERER) + " version: " + Gdx.gl20.glGetString(GL_VERSION);
	}

	private static String buildOSName() {
		return System.getProperty("os.name") + " (" + System.getProperty("os.version") + ") " + System.getProperty("os.arch");
	}
}
