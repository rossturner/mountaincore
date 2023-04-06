package technology.rocketjump.mountaincore.misc;

import com.codedisaster.steamworks.SteamAPI;

public class SteamUtils {

	public static com.codedisaster.steamworks.SteamUtils utils = new com.codedisaster.steamworks.SteamUtils(() -> {});

	public static boolean isRunningOnSteamDeck() {
		return SteamAPI.isSteamRunning() && utils.isSteamRunningOnSteamDeck();
	}

}
