package technology.rocketjump.mountaincore.logging;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lwjgl.opengl.Display;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.UserPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static technology.rocketjump.mountaincore.rendering.camera.GlobalSettings.VERSION;

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
		reportingEnabled = Boolean.valueOf(userPreferences.getPreference(UserPreferences.PreferenceKey.CRASH_REPORTING));
		messageDispatcher.addListener(this, MessageType.CRASH_REPORTING_OPT_IN_MODIFIED);
	}

	public static void displayCrashDialog(Throwable e) {
		Display.destroy(); //ensure opengl is destroyed

		Throwable exceptionToDisplay = e;
		if (e instanceof ProvisionException) {
			exceptionToDisplay = e.getCause();
		}

		Object[] options = new Object[] {
				"Copy to Clipboard",
				"Close"
		};
		try {
			String stackTrace = ExceptionUtils.getStackTrace(exceptionToDisplay);
			int lastNewLineIndex = 0;
			int lineCount = 5;
			while (lineCount > 0 && lastNewLineIndex != -1) {
				lastNewLineIndex = stackTrace.indexOf('\n', lastNewLineIndex + 1);
				lineCount--;
			}

			if (lastNewLineIndex > 0 && lastNewLineIndex < stackTrace.length()) {
				stackTrace = stackTrace.substring(0, lastNewLineIndex);
			}

			int result = JOptionPane.showOptionDialog(null, stackTrace, "Error - Game",
					JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);

			if (result == 0) {
				Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				systemClipboard.setContents(new StringSelection(getJsonPayload(e).toString(SerializerFeature.PrettyFormat)), null);
			}
		} catch (RuntimeException runtimeException) {
			CrashHandler.logCrash(new RuntimeException("Error displaying exception dialog", runtimeException));
		}
	}

	public boolean isOptInConfirmationRequired() {
		return userPreferences.getPreference(UserPreferences.PreferenceKey.CRASH_REPORTING) == null;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.CRASH_REPORTING_OPT_IN_MODIFIED: {
				Boolean reportingEnabled = (Boolean) msg.extraInfo;
				userPreferences.setPreference(UserPreferences.PreferenceKey.CRASH_REPORTING, String.valueOf(reportingEnabled));
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
			JSONObject payload = getJsonPayload(exception);

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

	public static JSONObject getJsonPayload(Throwable exception) {
		JSONObject payload = new JSONObject();
		payload.put("gameVersion", VERSION.toString());
		payload.put("displaySettings", Gdx.graphics.getDisplayMode(Gdx.graphics.getMonitor()).toString());
		payload.put("stackTrace", ExceptionUtils.getStackTrace(exception));
		payload.put("preferencesJson", removeKeybindings(UserPreferences.preferencesJson));
		return payload;
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

}
