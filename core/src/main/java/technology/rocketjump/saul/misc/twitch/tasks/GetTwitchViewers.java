package technology.rocketjump.saul.misc.twitch.tasks;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import technology.rocketjump.saul.misc.twitch.TwitchDataStore;
import technology.rocketjump.saul.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.saul.misc.twitch.model.TwitchViewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class GetTwitchViewers implements Callable<List<TwitchViewer>> {

	private final TwitchDataStore twitchDataStore;

	public GetTwitchViewers(TwitchDataStore twitchDataStore) {
		this.twitchDataStore = twitchDataStore;
	}

	@Override
	public List<TwitchViewer> call() throws Exception {
		TwitchAccountInfo accountInfo = twitchDataStore.getAccountInfo();
		if (accountInfo == null) {
			throw new Exception("Account info is null");
		}

		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder()
				.url("https://tmi.twitch.tv/group/user/" + accountInfo.getLogin() + "/chatters")
				.get()
				.build();

		Response response = client.newCall(request).execute();
		try {
//			if (GlobalSettings.DEV_MODE) {
//				Logger.debug("Request to " + request.url().toString() + " returned " + response.code());
//			}

			if (response.isSuccessful()) {
				List<TwitchViewer> viewers = new ArrayList<>();
				JSONObject responseJson = JSON.parseObject(response.body().string());
				JSONObject chatters = responseJson.getJSONObject("chatters");

				for (String chatterType : Arrays.asList("vips", "moderators", "viewers")) {
					for (Object nameObj : chatters.getJSONArray(chatterType)) {
						String username = nameObj.toString();
						if (!username.endsWith("bot")) {
							viewers.add(new TwitchViewer(username));
						}
					}
				}
				return viewers;
			} else {
				throw new Exception("Received " + response.code() + " while calling " + this.getClass().getSimpleName());
			}
		} finally {
			IOUtils.closeQuietly(response);
		}
	}
}
