package technology.rocketjump.mountaincore.misc.twitch.tasks;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import technology.rocketjump.mountaincore.misc.twitch.TwitchDataStore;
import technology.rocketjump.mountaincore.misc.twitch.TwitchRequestHandler;
import technology.rocketjump.mountaincore.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.mountaincore.misc.twitch.model.TwitchViewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class GetTwitchViewers implements Callable<List<TwitchViewer>> {

	private final TwitchRequestHandler twitchRequestHandler = new TwitchRequestHandler();
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

		Response response = twitchRequestHandler.get("https://api.twitch.tv/helix/chat/chatters?first=1000&broadcaster_id="+accountInfo.getUser_id()+"&moderator_id="+accountInfo.getUser_id(), twitchDataStore);

		try {
			if (response.isSuccessful()) {
				List<TwitchViewer> viewers = new ArrayList<>();
				JSONObject responseJson = JSON.parseObject(response.body().string());
				JSONArray data = responseJson.getJSONArray("data");

				for (int cursor = 0; cursor < data.size(); cursor++) {
					String username = data.getJSONObject(cursor).getString("user_name");
					if (!twitchDataStore.isBotAccount(username.toLowerCase(Locale.ROOT)) && !username.endsWith("bot")) {
						viewers.add(new TwitchViewer(username));
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
