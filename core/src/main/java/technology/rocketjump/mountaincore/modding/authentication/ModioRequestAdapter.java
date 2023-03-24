package technology.rocketjump.mountaincore.modding.authentication;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.modding.model.WrappedModioJson;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;

import java.io.IOException;
import java.util.List;

@Singleton
public class ModioRequestAdapter {

	private static final String MODIO_API_BASE_URL = "https://api.mod.io/v1";
	private static final String MODIO_API_KEY = "7aa85917a6decae0093157bd668d64c5"; // This is read-only access so probably okay to include here
	private static final String MODIO_GAME_ID = "17";

	private final OkHttpClient client = new OkHttpClient();

	@Inject
	public ModioRequestAdapter() {

	}

	public void emailCodeRequest(String emailAddress, Callback callback) {
		RequestBody formBody = new FormBody.Builder()
				.add("email", emailAddress)
				.build();

		Request request = new Request.Builder()
				.url(MODIO_API_BASE_URL + "/oauth/emailrequest?api_key=" + MODIO_API_KEY)
				.post(formBody)
				.build();

		client.newCall(request).enqueue(callback);
	}

	public void emailSodeSubmit(String code, Callback callback) {
		RequestBody formBody = new FormBody.Builder()
				.add("security_code", code)
				.build();

		Request request = new Request.Builder()
				.url(MODIO_API_BASE_URL + "/oauth/emailexchange?api_key=" + MODIO_API_KEY)
				.post(formBody)
				.build();

		client.newCall(request).enqueue(callback);
	}

	public void steamAuthRequest(byte[] encryptedAppTicket, boolean termsAgreed, Callback callback) {
		RequestBody formBody = new FormBody.Builder()
				.add("appdata", Base64.encodeBase64String(encryptedAppTicket))
				.add("terms_agreed", String.valueOf(termsAgreed))
				.build();

		Request request = new Request.Builder()
				.url(MODIO_API_BASE_URL + "/external/steamauth?api_key=" + MODIO_API_KEY)
				.post(formBody)
				.build();

		if (GlobalSettings.DEV_MODE) {
			Logger.debug("Steam Auth Request: " + request.toString());
		}
		client.newCall(request).enqueue(callback);
	}

	public Response downloadFile(String binaryUrl) throws IOException {
		Request request = new Request.Builder()
				.url(binaryUrl)
				.build();

		return client.newCall(request).execute();
	}

	public void termsConditions(String langCode, Callback callback) {
		Request request = new Request.Builder()
				.url(MODIO_API_BASE_URL + "/authenticate/terms?api_key=" + MODIO_API_KEY)
				.header("Accept-Language", langCode)
				.build();

		client.newCall(request).enqueue(callback);
	}

	// TODO this endpoint has a max result limit of 100 and should be implemented to use the pagination
	public List<WrappedModioJson> getSubscribedMods(String accessToken) throws IOException {
		Request request = new Request.Builder()
				.url(MODIO_API_BASE_URL + "/me/subscribed?game_id=" + MODIO_GAME_ID)
				.header("Authorization", "Bearer " + accessToken)
				.build();
		try (Response response = client.newCall(request).execute()) {
			if (response.isSuccessful()) {
				JSONObject responseBody = JSONObject.parseObject(response.body().string());
				return responseBody.getJSONArray("data").stream()
						.map(o -> new WrappedModioJson((JSONObject) o))
						.toList();
			} else {
				Logger.error("Failed to get subscribed mods: " + response.body().string());
				throw new IOException("Request to mod.io subscriptions failed: " + response.code());
			}
		}
	}
}
