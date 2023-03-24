package technology.rocketjump.mountaincore.misc.twitch.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import technology.rocketjump.mountaincore.misc.twitch.TwitchDataStore;
import technology.rocketjump.mountaincore.misc.twitch.TwitchRequestHandler;
import technology.rocketjump.mountaincore.misc.twitch.model.TwitchAccountInfo;

import java.util.concurrent.Callable;

public class GetTwitchAccountInfo implements Callable<TwitchAccountInfo> {

	private final TwitchRequestHandler twitchRequestHandler = new TwitchRequestHandler();
	private final TwitchDataStore twitchDataStore;

	public GetTwitchAccountInfo(TwitchDataStore twitchDataStore) {
		this.twitchDataStore = twitchDataStore;
	}

	@Override
	public TwitchAccountInfo call() throws Exception {
		Response response = twitchRequestHandler.get("https://id.twitch.tv/oauth2/validate", twitchDataStore);

		try {
			if (response.isSuccessful()) {
				return new ObjectMapper().readValue(response.body().string(), TwitchAccountInfo.class);
			} else {
				throw new Exception("Received " + response.code() + " while calling " + this.getClass().getSimpleName());
			}
		} finally {
			IOUtils.closeQuietly(response);
		}
	}

}
