package technology.rocketjump.saul.modding.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;

@Singleton
public class ModioRequestAdapter {

	private static final String MODIO_API_BASE_URL = "https://api.mod.io/v1";
	private static final String MODIO_API_KEY = "7aa85917a6decae0093157bd668d64c5"; // This is read-only access so probably okay to include here

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

	public void termsConditions(String langCode, Callback callback) {
		Request request = new Request.Builder()
				.url(MODIO_API_BASE_URL + "/authenticate/terms?api_key=" + MODIO_API_KEY)
				.header("Accept-Language", langCode)
				.build();

		client.newCall(request).enqueue(callback);
	}
}
