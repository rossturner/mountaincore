package technology.rocketjump.mountaincore.modding.authentication;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.net.HttpStatus;
import com.codedisaster.steamworks.SteamAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.persistence.UserPreferences;

import java.io.IOException;
import java.time.Instant;

@Singleton
public class ModioAuthManager {

	private static final String MODIO_GAME_URL = "https://mod.io/g/mountaincore";
	public static final int MODIO_ERROR_REFERENCE_TERMS_ACCEPTANCE_REQUIRED = 11074;
	private final ModioRequestAdapter modioRequestAdapter;
	private final UserPreferences userPreferences;

	private boolean userIsAuthenticated;
	private String accessToken;
	private Instant accessTokenExpiry;

	@Inject
	public ModioAuthManager(ModioRequestAdapter modioRequestAdapter, UserPreferences userPreferences) {
		this.modioRequestAdapter = modioRequestAdapter;
		this.userPreferences = userPreferences;

		loadTokenFromPreferences();
	}

	private void loadTokenFromPreferences() {
		Instant storedExpiry = Instant.ofEpochSecond(Long.valueOf(userPreferences.getPreference(UserPreferences.PreferenceKey.MODIO_ACCESS_TOKEN_EXPIRY)));
		if (storedExpiry.isAfter(Instant.now())) {
			this.accessTokenExpiry = storedExpiry;
			this.accessToken = userPreferences.getPreference(UserPreferences.PreferenceKey.MODIO_ACCESS_TOKEN);
			this.userIsAuthenticated = true;
		}
	}

	public String getAccessToken() {
		return accessToken;
	}

	public boolean isUserAuthenticated() {
		return userIsAuthenticated;
	}

	public String getModioGameHomepage() {
		if (SteamAPI.isSteamRunning()) {
			return MODIO_GAME_URL + "?portal=steam";
		} else {
			return MODIO_GAME_URL + "?portal=email";
		}
	}

	public void requestEmailCode(String emailAddress, Callback httpCallback) {
		modioRequestAdapter.emailCodeRequest(emailAddress, httpCallback);
	}

	public void submitEmailCode(String code, Runnable onCompletion) {
		modioRequestAdapter.emailSodeSubmit(code, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				Logger.error("Failed request to mod.io to submit email code", e);
				onCompletion.run();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (response.isSuccessful()) {
					parseTokenFromResponse(response);
				} else {
					Logger.error("Failed request to mod.io to submit email code: " + response.code() + " " + response.body().string());
				}
				onCompletion.run();
			}
		});
	}

	public void authenticateWithSteam(byte[] encryptedAppTicket, boolean termsAgreed, Runnable onCompletion, Runnable onError, Runnable onTermsRequired) {
		modioRequestAdapter.steamAuthRequest(encryptedAppTicket, termsAgreed, new Callback() {
			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (response.isSuccessful()) {
					parseTokenFromResponse(response);
					onCompletion.run();
				} else {
					JSONObject responseBody = JSON.parseObject(response.body().string());
					if (response.code() == HttpStatus.SC_FORBIDDEN && responseBody.getJSONObject("error").getIntValue("error_ref") == MODIO_ERROR_REFERENCE_TERMS_ACCEPTANCE_REQUIRED) {
						onTermsRequired.run();
					} else {
						Logger.error("Failed request to mod.io to authenticate with Steam: " + responseBody);
						onError.run();
					}
				}
			}

			@Override
			public void onFailure(Call call, IOException e) {
				Logger.error("Failed request to mod.io to authenticate with Steam", e);
				onError.run();
			}
		});
	}

	private void parseTokenFromResponse(Response response) throws IOException {
		JSONObject responseBody = JSON.parseObject(response.body().string());
		userPreferences.setPreference(UserPreferences.PreferenceKey.MODIO_ACCESS_TOKEN, responseBody.getString("access_token"));
		userPreferences.setPreference(UserPreferences.PreferenceKey.MODIO_ACCESS_TOKEN_EXPIRY, String.valueOf(responseBody.getLongValue("date_expires")));
		loadTokenFromPreferences();
	}
}
